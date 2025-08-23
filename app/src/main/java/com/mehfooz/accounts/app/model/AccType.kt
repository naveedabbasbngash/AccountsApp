package com.mehfooz.accounts.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AccType")
data class AccType(
    @PrimaryKey val AccTypeID: Long,
    val AccTypeName: String?,
    val AccTypeNameu: String?,
    val FLAG: String?
)