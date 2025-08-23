package com.mehfooz.accounts.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

object DbManager {
    private const val TAG = "DbManager"

    fun paths(context: Context): Triple<File, File, File> {
        val live = AppDatabase.dbFile(context)
        val incoming = File(live.parentFile, "incoming.db")
        val backup = File(live.parentFile, "backup.db")
        return Triple(live, incoming, backup)
    }

    fun integrityCheck(dbFile: File): Boolean {
        if (!dbFile.exists()) return false
        return try {
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            val c = db.rawQuery("PRAGMA integrity_check;", null)
            val ok = c.moveToFirst() && c.getString(0).equals("ok", true)
            c.close(); db.close()
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed: ${e.message}")
            false
        }
    }

    /** Room must be closed before calling this. */
    fun atomicSwap(context: Context, incoming: File): Boolean {
        val (live, _, backup) = paths(context)
        require(incoming.exists()) { "Incoming DB missing: ${incoming.absolutePath}" }
        require(integrityCheck(incoming)) { "Incoming DB integrity_check != ok" }

        AppDatabase.closeIfOpen()

        if (backup.exists()) backup.delete()
        if (live.exists() && !live.renameTo(backup)) {
            Log.e(TAG, "Failed to rename live -> backup")
            return false
        }
        if (!incoming.renameTo(live)) {
            Log.e(TAG, "Failed to rename incoming -> live; restoring backup")
            if (backup.exists()) backup.renameTo(live)
            return false
        }
        return try {
            AppDatabase.get(context).close()
            if (backup.exists()) backup.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Room open failed after swap: ${e.message}. Restoring backup.")
            if (live.exists()) live.delete()
            if (backup.exists()) backup.renameTo(live)
            false
        } finally {
            AppDatabase.closeIfOpen()
        }
    }
}