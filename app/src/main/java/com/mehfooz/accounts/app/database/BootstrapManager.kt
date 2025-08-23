package com.mehfooz.accounts.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object BootstrapManager {
    private const val TAG = "Bootstrap"

    private val http by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    // -------------------------------- Public APIs --------------------------------

    suspend fun bootstrapFromFull(
        context: Context,
        url: String,
        onProgress: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val (live, incoming, _) = DbManager.paths(context)

        downloadToFile(url, incoming, onProgress)
        require(DbManager.integrityCheck(incoming)) { "Incoming DB failed integrity_check" }
        require(hasRequiredTables(incoming)) { "Incoming DB missing required tables" }

        normalizeIncomingSchemaToRoom(incoming)

        require(DbManager.atomicSwap(context, incoming)) { "Atomic swap failed" }
        seedMetaDefaults(context)

        Log.i(TAG, "Bootstrap complete → ${live.absolutePath}")
    }

    suspend fun importFromAsset(
        context: Context,
        assetPath: String,               // e.g. "live_seed.sqlite" under app/src/main/assets/
        onProgress: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val (live, incoming, _) = DbManager.paths(context)

        // Copy asset -> incoming.db
        onProgress?.invoke(1)
        context.assets.open(assetPath).use { input ->
            incoming.parentFile?.mkdirs()
            if (incoming.exists()) incoming.delete()
            FileOutputStream(incoming).use { output ->
                val buf = ByteArray(1024 * 1024)
                var r: Int
                var total = 0L
                while (true) {
                    r = input.read(buf)
                    if (r <= 0) break
                    output.write(buf, 0, r)
                    total += r
                    onProgress?.invoke(((total % 90_000) / 900).toInt().coerceIn(1, 90))
                }
                output.fd.sync()
            }
        }
        onProgress?.invoke(92)

        // Validate + normalize for Room
        require(DbManager.integrityCheck(incoming)) { "Asset DB failed integrity_check" }
        require(hasRequiredTables(incoming)) { "Asset DB missing required tables" }
        normalizeIncomingSchemaToRoom(incoming) // ensures PKs, indices, and Status column
        onProgress?.invoke(96)

        // Swap incoming -> live
        require(DbManager.atomicSwap(context, incoming)) { "Atomic swap failed" }
        seedMetaDefaults(context)
        onProgress?.invoke(100)

        Log.i(TAG, "Local asset import complete → ${live.absolutePath}")
    }

    // ------------------------------- Helpers -------------------------------------

    private suspend fun downloadToFile(
        url: String,
        outFile: File,
        onProgress: ((Int) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        outFile.parentFile?.mkdirs()
        if (outFile.exists()) outFile.delete()

        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { res: Response ->
            if (!res.isSuccessful) {
                val bodyStr = res.body?.string().orEmpty()
                throw IllegalStateException("HTTP ${res.code} ${res.message}\n$bodyStr")
            }
            val body = res.body ?: error("Empty body")
            val total = body.contentLength().coerceAtLeast(1L)
            var readSoFar = 0L
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    val buf = ByteArray(2 * 1024 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        readSoFar += n
                        onProgress?.invoke(((readSoFar * 100) / total).toInt().coerceIn(0, 99))
                    }
                    output.fd.sync()
                }
            }
            onProgress?.invoke(100)
        }
    }

    private fun hasRequiredTables(dbFile: File): Boolean {
        return try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                fun exists(name: String): Boolean {
                    return db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        arrayOf(name)
                    ).use { c -> c.moveToFirst() }
                }

                val ok = exists("Acc_Personal") && exists("AccType") && exists("Transactions_P")
                ok  // <-- return this from the lambda
            }
        } catch (e: Exception) {
            Log.e(TAG, "Schema check failed: ${e.message}")
            false
        }
    }
    /**
     * Normalize the imported DB so Room validation passes:
     *  - Ensure PKs are INTEGER NOT NULL PRIMARY KEY (Acc_Personal, AccType, Transactions_P)
     *  - Ensure Transactions_P has a TEXT column named Status (nullable is fine)
     *  - Ensure indices on Transactions_P(TDate) and Transactions_P(AccID)
     *  - Ensure Meta(key TEXT PK, value TEXT) exists (Room expects it)
     */
    private fun normalizeIncomingSchemaToRoom(incoming: File) {
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            incoming.absolutePath, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )

        fun tableExists(name: String): Boolean =
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(name)
            ).use { it.moveToFirst() }

        fun columnExists(table: String, column: String): Boolean =
            db.rawQuery("PRAGMA table_info($table);", null).use { c ->
                val idxName = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    if (c.getString(idxName).equals(column, true)) return@use true
                }
                false
            }

        fun isPkNotNull(table: String, pkName: String): Boolean =
            db.rawQuery("PRAGMA table_info($table);", null).use { c ->
                val nameIdx = c.getColumnIndex("name")
                val notNullIdx = c.getColumnIndex("notnull")
                val pkIdx = c.getColumnIndex("pk")
                var ok = false
                while (c.moveToNext()) {
                    val n = c.getString(nameIdx)
                    val nn = c.getInt(notNullIdx)
                    val pk = c.getInt(pkIdx)
                    if (n.equals(pkName, ignoreCase = true) && pk == 1) {
                        ok = (nn == 1)
                        break
                    }
                }
                ok
            }

        // ---- Acc_Personal: PK NOT NULL ----
        fun recreateAccPersonalIfNeeded() {
            if (tableExists("Acc_Personal") && isPkNotNull("Acc_Personal", "AccID")) return
            if (!tableExists("Acc_Personal")) return // nothing to do if it's not even there
            db.beginTransaction()
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS Acc_Personal_new (
                      AccID INTEGER NOT NULL PRIMARY KEY,
                      RDate TEXT,
                      Name TEXT,
                      Phone TEXT,
                      Fax TEXT,
                      Address TEXT,
                      Description TEXT,
                      UAccName TEXT,
                      statusg TEXT,
                      UserID INTEGER,
                      CompanyID INTEGER,
                      WName TEXT
                    );
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO Acc_Personal_new
                    (AccID,RDate,Name,Phone,Fax,Address,Description,UAccName,statusg,UserID,CompanyID,WName)
                    SELECT AccID,RDate,Name,Phone,Fax,Address,Description,UAccName,statusg,UserID,CompanyID,WName
                    FROM Acc_Personal;
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE Acc_Personal;")
                db.execSQL("ALTER TABLE Acc_Personal_new RENAME TO Acc_Personal;")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }

        // ---- AccType: PK NOT NULL ----
        fun recreateAccTypeIfNeeded() {
            if (tableExists("AccType") && isPkNotNull("AccType", "AccTypeID")) return
            if (!tableExists("AccType")) return
            db.beginTransaction()
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS AccType_new (
                      AccTypeID INTEGER NOT NULL PRIMARY KEY,
                      AccTypeName TEXT,
                      AccTypeNameu TEXT,
                      FLAG TEXT
                    );
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO AccType_new
                    (AccTypeID,AccTypeName,AccTypeNameu,FLAG)
                    SELECT AccTypeID,AccTypeName,AccTypeNameu,FLAG
                    FROM AccType;
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE AccType;")
                db.execSQL("ALTER TABLE AccType_new RENAME TO AccType;")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }

        // ---- Transactions_P: PK NOT NULL, Status TEXT column, required indices ----
        fun recreateTransactionsPIfNeededOrAddStatus() {
            val hasTable = tableExists("Transactions_P")
            if (!hasTable) return

            val pkOk = isPkNotNull("Transactions_P", "VoucherNo")
            val hasStatus = columnExists("Transactions_P", "Status")

            // If PK is good and Status missing => cheap ALTER TABLE to add the column
            if (pkOk && !hasStatus) {
                db.execSQL("ALTER TABLE Transactions_P ADD COLUMN Status TEXT;")
                // ensure indices
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_TDate ON Transactions_P(TDate);")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_AccID ON Transactions_P(AccID);")
                return
            }

            // If PK is NOT correct OR we want to guarantee shape in one pass => recreate table
            if (!pkOk || !hasStatus) {
                db.beginTransaction()
                try {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS Transactions_P_new (
                          VoucherNo INTEGER NOT NULL PRIMARY KEY,
                          TDate TEXT,
                          AccID INTEGER,
                          AccTypeID INTEGER,
                          Description TEXT,
                          DrCents INTEGER,
                          CrCents INTEGER,
                          Status TEXT
                        );
                        """.trimIndent()
                    )

                    if (hasStatus) {
                        db.execSQL(
                            """
                            INSERT INTO Transactions_P_new
                            (VoucherNo,TDate,AccID,AccTypeID,Description,DrCents,CrCents,Status)
                            SELECT VoucherNo,TDate,AccID,AccTypeID,Description,DrCents,CrCents,Status
                            FROM Transactions_P;
                            """.trimIndent()
                        )
                    } else {
                        db.execSQL(
                            """
                            INSERT INTO Transactions_P_new
                            (VoucherNo,TDate,AccID,AccTypeID,Description,DrCents,CrCents,Status)
                            SELECT VoucherNo,TDate,AccID,AccTypeID,Description,DrCents,CrCents,NULL
                            FROM Transactions_P;
                            """.trimIndent()
                        )
                    }

                    db.execSQL("DROP TABLE Transactions_P;")
                    db.execSQL("ALTER TABLE Transactions_P_new RENAME TO Transactions_P;")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_TDate ON Transactions_P(TDate);")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_AccID ON Transactions_P(AccID);")
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
                return
            }

            // If we get here, PK ok and Status already existed; just ensure indices.
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_TDate ON Transactions_P(TDate);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_TP_AccID ON Transactions_P(AccID);")
        }

        // ---- Meta: ensure table exists (Room expects it) ----
        fun ensureMetaTable() {
            if (!tableExists("Meta")) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `Meta` (
              `key`   TEXT NOT NULL PRIMARY KEY,
              `value` TEXT NOT NULL
            );
            """.trimIndent()
                )
                return
            }
            // optional: ensure columns exist (older seeds)
            db.rawQuery("PRAGMA table_info(`Meta`);", null).use { c ->
                val names = buildList {
                    val idx = c.getColumnIndex("name")
                    while (c.moveToNext()) add(c.getString(idx))
                }
                if (!names.contains("key") || !names.contains("value")) {
                    db.execSQL("DROP TABLE IF EXISTS `Meta`;")
                    db.execSQL(
                        """
                CREATE TABLE `Meta` (
                  `key`   TEXT NOT NULL PRIMARY KEY,
                  `value` TEXT NOT NULL
                );
                """.trimIndent()
                    )
                }
            }
        }
        recreateAccPersonalIfNeeded()
        recreateAccTypeIfNeeded()
        recreateTransactionsPIfNeededOrAddStatus()
        ensureMetaTable()

        db.close()
    }

    private fun seedMetaDefaults(context: Context) {
        val db = AppDatabase.get(context)
        val dao = db.meta()
        runBlocking {
            if (dao.get("lastVersion") == null) dao.put(Meta("lastVersion", "0"))
            if (dao.get("schemaVersion") == null) dao.put(Meta("schemaVersion", "1"))
            if (dao.get("buildUtc") == null) dao.put(Meta("buildUtc", nowIsoUtc()))
        }
    }

    private fun nowIsoUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(System.currentTimeMillis())
    }
}