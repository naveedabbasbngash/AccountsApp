package com.mehfooz.accounts.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Meta")
data class Meta(
    @PrimaryKey val key: String,
    val value: String
)