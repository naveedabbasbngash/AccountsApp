package com.mehfooz.accounts.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehfooz.accounts.app.data.AppDatabase
import com.mehfooz.accounts.app.data.TransactionsPDao
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TxFilter { ALL, DEBIT, CREDIT }

/** Row used by the list */
data class TxItemUi(
    val voucherNo: Long,
    val date: String,
    val name: String,
    val description: String,
    val drUnits: Float,   // debit in UNITS
    val crUnits: Float,   // credit in UNITS
    val currency: String  // e.g., "USD", "PKR"
) {
    val isCredit: Boolean get() = crUnits > 0f
    val amountUnits: Float get() = if (isCredit) crUnits else drUnits
}

/** Single summary (if you still need a one-line person summary somewhere) */
data class BalanceUi(
    val accId: Long,
    val name: String,
    val currency: String,   // AccTypeName (can be generic if you aggregate)
    val creditUnits: Float,
    val debitUnits: Float,
    val balanceUnits: Float // credit - debit
)

/** Per-currency rows for the searched person (used by the Balance tab) */
data class BalanceCurrencyUi(
    val currency: String,
    val creditUnits: Float,
    val debitUnits: Float,
    val balanceUnits: Float    // credit - debit
)

@OptIn(FlowPreview::class)
class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: TransactionsPDao = AppDatabase.get(app).transactionsP()

    /* ---------------- UI state ---------------- */

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    private val _filter = MutableStateFlow(TxFilter.ALL)
    val filter: StateFlow<TxFilter> = _filter

    // (startIso, endIso) in yyyy-MM-dd or nulls
    private val _dateRange = MutableStateFlow<Pair<String?, String?>>(null to null)
    val dateRange: StateFlow<Pair<String?, String?>> = _dateRange

    fun setSearch(s: String) { _search.value = s }
    fun setFilter(f: TxFilter) { _filter.value = f }
    fun setDateRange(startIso: String?, endIso: String?) { _dateRange.value = startIso to endIso }

    /* ---------------- Mappers ---------------- */

    private fun TransactionsPDao.TxRow.toUi(): TxItemUi =
        TxItemUi(
            voucherNo   = voucherNo,
            date        = date,
            name        = name,
            description = description.orEmpty(),
            drUnits     = drCents / 100f,
            crUnits     = crCents / 100f,
            currency    = currency
        )

    private fun TransactionsPDao.PersonBalanceRow.toUi(): BalanceUi {
        val cr = crCents / 100f
        val dr = drCents / 100f
        return BalanceUi(
            accId        = accId,
            name         = name,
            currency     = currency,
            creditUnits  = cr,
            debitUnits   = dr,
            balanceUnits = cr - dr
        )
    }

    private fun TransactionsPDao.BalanceCurrencyRow.toUi(): BalanceCurrencyUi =
        BalanceCurrencyUi(
            currency     = currency,
            creditUnits  = creditUnits,
            debitUnits   = debitUnits,
            balanceUnits = creditUnits - debitUnits
        )

    /* ---------------- Flows for UI ---------------- */

    // Transactions list respecting search, chip filter and optional date range
    val items: StateFlow<List<TxItemUi>> =
        combine(
            search.debounce(300).map { it.trim() },
            filter,
            dateRange
        ) { q, f, dr -> Triple(q, f, dr) }
            .flatMapLatest { (q, f, dr) ->
                val (start, end) = dr
                dao.lastTransactions(
                    limit     = 100,
                    name      = if (q.isBlank()) null else q,
                    only      = f.name,      // "ALL" / "DEBIT" / "CREDIT"
                    startDate = start,       // yyyy-MM-dd or null
                    endDate   = end          // yyyy-MM-dd or null
                )
            }
            .map { rows -> rows.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Single-line balance summary for the searched name (if needed).
    // When search is blank → null (UI can hide it).
    val balance: StateFlow<BalanceUi?> =
        combine(
            search.debounce(300).map { it.trim() },
            dateRange
        ) { q, dr -> Pair(q, dr) }
            .flatMapLatest { (q, dr) ->
                val (start, end) = dr
                if (q.isBlank()) {
                    flowOf<BalanceUi?>(null)
                } else {
                    dao.personBalance(name = q, start = start, end = end)
                        .map { it?.toUi() }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Per-currency balance list for the searched name (drives your Balance tab)
    val balanceByCurrency: StateFlow<List<BalanceCurrencyUi>> =
        combine(
            search.debounce(300).map { it.trim() },
            dateRange
        ) { q, dr -> q to dr }
            .flatMapLatest { (q, dr) ->
                if (q.isBlank()) {
                    flowOf(emptyList())
                } else {
                    val (start, end) = dr
                    dao.balanceByCurrencyForName(
                        name      = q,
                        startDate = start,
                        endDate   = end
                    )
                }
            }
            .map { rows -> rows.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}