package com.mehfooz.accounts.app.data

import androidx.room.*

@Dao
interface AccTypeDao {
    @Query("SELECT * FROM AccType ORDER BY AccTypeName COLLATE NOCASE ASC")
    suspend fun listAll(): List<AccType>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg rows: AccType)

    @Query("DELETE FROM AccType WHERE AccTypeID = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM AccType")
    suspend fun count(): Long
}