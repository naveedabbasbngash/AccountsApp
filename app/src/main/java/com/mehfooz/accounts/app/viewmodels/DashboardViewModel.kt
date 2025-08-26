package com.mehfooz.accounts.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.DailyDebitsCredits
import com.mehfooz.accounts.app.data.MonthTotals
import com.mehfooz.accounts.app.model.DayValue
import kotlinx.coroutines.flow.*

/** Tabs you already use */
enum class MonthTab { THIS_MONTH, LAST_MONTH }

/** Slice for pies */
data class PieSlice(val label: String, val value: Float)

/** One pie per currency */
data class CurrencyPieBucket(
    val currency: String,                 // e.g. "USD"
    val currencySymbol: String = "",      // optional (leave "" if unknown)
    val slices: List<PieSlice>            // [Credit, Debit] or multiple if you extend later
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionsP()

    // --- tabs ---
    private val _tab = MutableStateFlow(MonthTab.THIS_MONTH)
    val tab: StateFlow<MonthTab> = _tab
    fun setTab(t: MonthTab) { _tab.value = t }

    // year-month list like ["2025-08","2025-07",...]
    private val months: StateFlow<List<String>> =
        dao.recentYearMonths(limit = 24)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // selected year-month based on tab
    private val selectedYm: StateFlow<String?> =
        combine(months, _tab) { ms, t ->
            if (ms.isEmpty()) null
            else when (t) {
                MonthTab.THIS_MONTH -> ms.getOrNull(0)
                MonthTab.LAST_MONTH -> ms.getOrNull(1) ?: ms.getOrNull(0)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --------- Line chart daily series (you already had this) ----------
    val dailyDC: StateFlow<List<DailyDebitsCredits>> =
        selectedYm.flatMapLatest { ym ->
            if (ym == null) emptyFlow() else dao.dailyDebitsCreditsByYearMonth(ym)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val credits: StateFlow<List<DayValue>> =
        dailyDC.map { rows -> rows.sortedBy { it.day }.map { DayValue(it.day, it.crUnits) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debits: StateFlow<List<DayValue>> =
        dailyDC.map { rows -> rows.sortedBy { it.day }.map { DayValue(it.day, it.drUnits) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --------- Pie: Credit vs Debit for selected month ----------
    val pieData: StateFlow<List<PieSlice>> =
        selectedYm.flatMapLatest { ym ->
            if (ym == null) {
                flowOf(emptyList())
            } else {
                dao.monthTotals(ym).map { totals: MonthTotals ->
                    listOf(
                        PieSlice("Credit", totals.crUnits.toFloat()),
                        PieSlice("Debit",  totals.drUnits.toFloat())
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --------- Pie(s) by Currency (LEFT JOIN AccType) ----------
    // DAO method name assumed: sumsByCurrency(ym) -> Flow<List<CurrencyBreak>>
    // If you haven't added it yet, the `catch` below keeps app compiling/running.
    data class CurrencyBreak(
        val currency: String, // AccType.AccTypeName (or whatever holds “currency”)
        val crUnits: Double,
        val drUnits: Double
    )

    val pieByCurrency: StateFlow<List<CurrencyPieBucket>> =
        selectedYm.flatMapLatest { ym ->
            if (ym == null) {
                flowOf(emptyList())
            } else {
                // Try to call a DAO query. If not present yet, fallback to empty.
                // Replace `dao.sumsByCurrency(ym)` with your actual DAO function name once added.
                (runCatching { dao.sumsByCurrency(ym) } // <-- add this DAO; see snippet below
                    .getOrNull() ?: flowOf(emptyList()))
                    .map { rows ->
                        rows.map { r ->
                            CurrencyPieBucket(
                                currency = r.currency.ifBlank { "Unknown" },
                                currencySymbol = symbolFor(r.currency),
                                slices = listOf(
                                    PieSlice("Credit", r.crUnits.toFloat()),
                                    PieSlice("Debit",  r.drUnits.toFloat())
                                )
                            )
                        }
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun symbolFor(currencyName: String): String =
        when (currencyName.uppercase()) {
            "USD", "US DOLLAR", "DOLLAR", "UNITED STATES DOLLAR" -> "$"
            "EUR", "EURO" -> "€"
            "GBP", "POUND" -> "£"
            "PKR", "RUPEE", "RS", "RUPEES" -> "Rs"
            "AED", "DIRHAM" -> "د.إ"
            "SAR", "RIYAL" -> "﷼"
            "INR" -> "₹"
            else -> "" // unknown / mixed
        }

    data class PieSlice(val label: String, val value: Float)

    data class CurrencyPieBucket(
        val currency: String,
        val currencySymbol: String,
        val slices: List<PieSlice>
    )
}