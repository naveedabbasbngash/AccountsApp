// DbHealth.kt
package com.mehfooz.accounts.app.data

import android.content.Context
import android.util.Log
import java.io.File

object DbHealth {
    private const val TAG = "DbHealth"

    suspend fun logState(context: Context) {
        val live = AppDatabase.dbFile(context)
        val size = if (live.exists()) live.length() else 0L
        Log.d(TAG, "live.db path=${live.absolutePath} exists=${live.exists()} size=$size bytes")

        try {
            val db = AppDatabase.get(context)
            val accCount = db.accPersonal().count()
            val typeCount = db.accType().count()
            val txCount = db.transactionsP().count()
            Log.d(TAG, "counts: Acc_Personal=$accCount AccType=$typeCount Transactions_P=$txCount")
        } catch (e: Exception) {
            Log.e(TAG, "open/query failed: ${e.message}", e)
        }
    }
}