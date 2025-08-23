package com.mehfooz.accounts.app.data

import androidx.room.*

@Dao
interface AccPersonalDao {
    @Query("SELECT * FROM Acc_Personal WHERE AccID = :id LIMIT 1")
    suspend fun getById(id: Long): AccPersonal?

    @Query("SELECT * FROM Acc_Personal ORDER BY Name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun listPaged(limit: Int, offset: Int): List<AccPersonal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg rows: AccPersonal)

    @Query("DELETE FROM Acc_Personal WHERE AccID = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM Acc_Personal")
    suspend fun count(): Long
}