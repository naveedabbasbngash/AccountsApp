package com.mehfooz.accounts.app.data

import androidx.room.*

@Dao
interface MetaDao {
    @Query("SELECT value FROM Meta WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: Meta)

    @Query("DELETE FROM Meta WHERE key = :key")
    suspend fun delete(key: String)
}