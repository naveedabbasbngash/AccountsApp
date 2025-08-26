package com.mehfooz.accounts.app.model

/** One slice for a pie chart. e.g., "Credit" 120.5f */
data class PieSlice(
    val label: String,
    val value: Float
)

/** One currency row: the currency name/symbol + its pie slices (credit/debit). */
data class CurrencyPieBucket(
    val currency: String,        // e.g., "USD", "PKR", "EUR"
    val currencySymbol: String,  // e.g., "$", "₨", "€"  (can be empty if unknown)
    val slices: List<PieSlice>   // usually two: [Credit, Debit]
)