package com.mehfooz.accounts.app.model;

/** One series point for the chart (UNITS = cents/100f). */
data class DayValue(
    val day: Int,            // 1..31
    val amountUnits: Float   // currency units
)