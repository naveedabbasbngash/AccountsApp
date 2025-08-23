package com.mehfooz.accounts.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Acc_Personal")
data class AccPersonal(
    @PrimaryKey val AccID: Long,
    val RDate: String?,       // TEXT (ISO datetime stored as text)
    val Name: String?,
    val Phone: String?,
    val Fax: String?,
    val Address: String?,
    val Description: String?,
    val UAccName: String?,
    val statusg: String?,
    val UserID: Long?,        // INTEGER NULL
    val CompanyID: Long?,     // INTEGER NULL
    val WName: String?
)