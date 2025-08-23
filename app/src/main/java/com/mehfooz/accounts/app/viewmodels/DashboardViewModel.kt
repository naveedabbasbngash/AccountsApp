package com.mehfooz.accounts.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.DailyDebitsCredits
import com.mehfooz.accounts.app.model.DayValue
import kotlinx.coroutines.flow.*

enum class MonthTab { THIS_MONTH, LAST_MONTH }

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionsP()

    private val _tab = MutableStateFlow(MonthTab.THIS_MONTH)
    val tab: StateFlow<MonthTab> = _tab
    fun setTab(t: MonthTab) { _tab.value = t }

    private val months: StateFlow<List<String>> =
        dao.recentYearMonths(limit = 24)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedYm: StateFlow<String?> =
        combine(months, _tab) { ms, t ->
            if (ms.isEmpty()) null
            else when (t) {
                MonthTab.THIS_MONTH -> ms.getOrNull(0)
                MonthTab.LAST_MONTH -> ms.getOrNull(1) ?: ms.getOrNull(0)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Debits & Credits per day from Room
    val dailyDC: StateFlow<List<DailyDebitsCredits>> =
        selectedYm.flatMapLatest { ym ->
            if (ym == null) emptyFlow() else dao.dailyDebitsCreditsByYearMonth(ym)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Convert to MPChart entries (DayValue)
    val credits: StateFlow<List<DayValue>> =
        dailyDC.map { rows -> rows.sortedBy { it.day }.map { DayValue(it.day, it.crUnits) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debits: StateFlow<List<DayValue>> =
        dailyDC.map { rows -> rows.sortedBy { it.day }.map { DayValue(it.day, it.drUnits) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}