package com.mehfooz.accounts.app.data

/**
 * One per calendar day.
 * day  = "yyyy-MM-dd"
 * netCents = (credits - debits) in cents
 */
data class DailyTotal(
    val day: String,
    val netCents: Long
)