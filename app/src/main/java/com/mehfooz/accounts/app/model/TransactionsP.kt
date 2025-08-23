package com.mehfooz.accounts.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Transactions_P",
    indices = [
        Index(value = ["TDate"], name = "idx_TP_TDate"),
        Index(value = ["AccID"], name = "idx_TP_AccID")
    ]
)
data class TransactionsP(
    @PrimaryKey val VoucherNo: Long,
    val TDate: String?,        // TEXT (ISO datetime)
    val AccID: Long?,          // INTEGER NULL
    val AccTypeID: Long?,      // INTEGER NULL
    val Description: String?,
    val DrCents: Long?,        // INTEGER NULL
    val CrCents: Long?,        // INTEGER NULL
    val Status: String?        // NEW column (nullable TEXT)
)