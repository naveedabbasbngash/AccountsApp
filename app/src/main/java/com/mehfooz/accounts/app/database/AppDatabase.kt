package com.mehfooz.accounts.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccPersonal::class,
        AccType::class,
        TransactionsP::class,
        Meta::class
    ],
    version = 2,                 // ✅ must be 2 to match AppDatabase_Impl
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accPersonal(): AccPersonalDao
    abstract fun accType(): AccTypeDao
    abstract fun transactionsP(): TransactionsPDao
    abstract fun meta(): MetaDao

    companion object {
        private const val DB_NAME = "live.db"
        @Volatile private var INSTANCE: AppDatabase? = null

        // Migration 1 → 2: add Status if it's missing
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Guarded ALTER (older SQLite ignores duplicate adds poorly, so check first)
                var hasStatus = false
                db.query("PRAGMA table_info(`Transactions_P`)").use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0 && c.getString(nameIdx).equals("Status", ignoreCase = true)) {
                            hasStatus = true; break
                        }
                    }
                }
                if (!hasStatus) {
                    db.execSQL("ALTER TABLE `Transactions_P` ADD COLUMN `Status` TEXT")
                }
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)                       // ✅ register migration
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }

        fun closeIfOpen() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun dbFile(context: Context) = context.getDatabasePath(DB_NAME)
    }
}