package com.mehfooz.accounts.app.data

import androidx.room.Dao
import androidx.room.Query
import com.mehfooz.accounts.app.ui.DashboardViewModel
import kotlinx.coroutines.flow.Flow

/* --- Existing response models you already use --- */
data class DailyDebitsCredits(
    val day: Int,
    val drUnits: Float,
    val crUnits: Float
)

data class CurrencyBreakdownRow(
    val label: String,   // currency label (FLAG or AccTypeName)
    val crUnits: Double, // total credits in UNITS for that currency
    val drUnits: Double  // total debits  in UNITS for that currency
)

data class MonthTotals(
    val drUnits: Double,
    val crUnits: Double
)

/* You already return Flow<List<DailyTotal>> elsewhere.
   Keep using your existing DailyTotal data class for that method. */

/* --- (NEW) joined variant model name to avoid collisions --- */
data class DailySumsJoined(
    val day: Int,
    val drUnits: Float,
    val crUnits: Float
)

/* (Existing) single-day totals for the date picker */
data class DayTotals(
    val crUnits: Float,
    val drUnits: Float
)

@Dao
interface TransactionsPDao {

    /* ------------------ basics you already had ------------------ */

    @Query("SELECT COUNT(*) FROM Transactions_P")
    suspend fun count(): Long

    @Query("""
        SELECT substr(TDate,1,7) AS ym
        FROM Transactions_P
        WHERE TDate IS NOT NULL
        GROUP BY ym
        ORDER BY ym DESC
        LIMIT :limit
    """)
    fun recentYearMonths(limit: Int = 24): Flow<List<String>>

    /* Grouped-by-day (no join), using Status -> debit/credit mapping */
    @Query("""
        SELECT CAST(substr(TDate, 9, 2) AS INTEGER) AS day,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(Status,'')) IN ('banam','banaam','benaam','benam','debit')
                       THEN COALESCE(DrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS drUnits,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(Status,'')) IN ('jama','credit')
                       THEN COALESCE(CrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS crUnits
        FROM Transactions_P
        WHERE TDate IS NOT NULL
          AND substr(TDate,1,7) = :ym
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailyDebitsCreditsByYearMonth(ym: String): Flow<List<DailyDebitsCredits>>

    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(Status,'')) IN ('banam','banaam','benaam','benam','debit')
                    THEN COALESCE(DrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS drUnits,
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(Status,'')) IN ('jama','credit')
                    THEN COALESCE(CrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS crUnits
        FROM Transactions_P
        WHERE TDate IS NOT NULL
          AND substr(TDate,1,7) = :ym
    """)
    fun monthTotals(ym: String): Flow<MonthTotals>

    /* Net by day (credits - debits); you already consume DailyTotal */
    @Query("""
        SELECT substr(TDate, 1, 10) AS day,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(Status,'')) IN ('jama','credit') 
                       THEN COALESCE(CrCents, 0)
                     WHEN lower(COALESCE(Status,'')) IN ('banam','banaam','benaam','benam','debit') 
                       THEN -COALESCE(DrCents, 0)
                     ELSE 0
                   END
               ),0) AS netCents
        FROM Transactions_P
        WHERE substr(TDate,1,7) = :ym
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailyTotalsByYearMonth(ym: String): Flow<List<DailyTotal>>

    /* Single calendar day (yyyy-MM-dd) totals for date picker */
    @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN lower(COALESCE(Status,'')) IN ('jama','credit') THEN COALESCE(CrCents,0) ELSE 0 END),0)/100.0 AS crUnits,
          COALESCE(SUM(CASE WHEN lower(COALESCE(Status,'')) IN ('banam','banaam','benaam','benam','debit') THEN COALESCE(DrCents,0) ELSE 0 END),0)/100.0 AS drUnits
        FROM Transactions_P
        WHERE TDate IS NOT NULL
          AND substr(TDate,1,10) = :date     /* yyyy-MM-dd */
    """)
    suspend fun totalsForDate(date: String): DayTotals


    /* ------------------ NEW: JOIN with AccType ------------------ */
    /* Simple joined version (no filter) — future‑proof: you can show labels or build filters later */
    @Query("""
        SELECT CAST(substr(tp.TDate, 9, 2) AS INTEGER) AS day,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(tp.Status,'')) IN ('banam','banaam','benaam','benam','debit')
                       THEN COALESCE(tp.DrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS drUnits,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(tp.Status,'')) IN ('jama','credit')
                       THEN COALESCE(tp.CrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS crUnits
        FROM Transactions_P tp
        LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
        WHERE tp.TDate IS NOT NULL
          AND substr(tp.TDate,1,7) = :ym
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailySumsByMonthJoined(ym: String): Flow<List<DailySumsJoined>>

    /* Joined + filter by specific AccTypeIDs */
    @Query("""
        SELECT CAST(substr(tp.TDate, 9, 2) AS INTEGER) AS day,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(tp.Status,'')) IN ('banam','banaam','benaam','benam','debit')
                       THEN COALESCE(tp.DrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS drUnits,
               COALESCE(SUM(
                   CASE
                     WHEN lower(COALESCE(tp.Status,'')) IN ('jama','credit')
                       THEN COALESCE(tp.CrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS crUnits
        FROM Transactions_P tp
        LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
        WHERE tp.TDate IS NOT NULL
          AND substr(tp.TDate,1,7) = :ym
          AND tp.AccTypeID IN (:typeIds)
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailySumsByMonthJoinedFilter(
        ym: String,
        typeIds: List<Long>
    ): Flow<List<DailySumsJoined>>

    /* (Optional) Month totals with join (no filter) */
    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(tp.Status,'')) IN ('banam','banaam','benaam','benam','debit')
                    THEN COALESCE(tp.DrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS drUnits,
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(tp.Status,'')) IN ('jama','credit')
                    THEN COALESCE(tp.CrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS crUnits
        FROM Transactions_P tp
        LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
        WHERE tp.TDate IS NOT NULL
          AND substr(tp.TDate,1,7) = :ym
    """)
    fun monthTotalsJoined(ym: String): Flow<MonthTotals>

    /* (Optional) Month totals with join + filter */
    @Query("""
        SELECT
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(tp.Status,'')) IN ('banam','banaam','benaam','benam','debit')
                    THEN COALESCE(tp.DrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS drUnits,
            COALESCE(SUM(
                CASE
                  WHEN lower(COALESCE(tp.Status,'')) IN ('jama','credit')
                    THEN COALESCE(tp.CrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS crUnits
        FROM Transactions_P tp
        LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
        WHERE tp.TDate IS NOT NULL
          AND substr(tp.TDate,1,7) = :ym
          AND tp.AccTypeID IN (:typeIds)
    """)
    fun monthTotalsJoinedFilter(
        ym: String,
        typeIds: List<Long>
    ): Flow<MonthTotals>


    @Query("""
        SELECT
            COALESCE(at.FLAG, at.AccTypeName, 'Unknown') AS label,
            -- sum credits
            COALESCE(SUM(
                CASE
                  WHEN lower(tp.Status) IN ('jama','credit') THEN COALESCE(tp.CrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS crUnits,
            -- sum debits
            COALESCE(SUM(
                CASE
                  WHEN lower(tp.Status) IN ('banam','benaam','benam','debit') THEN COALESCE(tp.DrCents, 0)
                  ELSE 0
                END
            ),0) / 100.0 AS drUnits
        FROM Transactions_P tp
        LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
        WHERE tp.TDate IS NOT NULL
          AND substr(tp.TDate,1,7) = :ym
        GROUP BY label
        HAVING (crUnits > 0.0 OR drUnits > 0.0)
        ORDER BY (crUnits + drUnits) DESC
    """)
    fun currencyBreakdownByYearMonth(ym: String): Flow<List<CurrencyBreakdownRow>>
    // 1) Result row
    data class CurrencyTotals(
        val accTypeId: Long,
        val name: String,
        val drUnits: Float,
        val crUnits: Float
    )

    /**
     * Per‑currency totals for the selected month (yyyy‑MM).
     * LEFT JOIN so currencies with zero activity can still appear (0,0).
     *
     * Version A (STATUS‑AWARE): uses Status to decide which column is effective
     *   - 'jama' → credit
     *   - 'banam'/'benaam'/'benam' → debit
     */
    @Query("""
SELECT 
  t.AccTypeID         AS accTypeId,
  at.AccTypeName      AS name,
  COALESCE(SUM(
      CASE WHEN lower(t.Status) IN ('banam','benaam','benam','debit')
           THEN COALESCE(t.DrCents, t.CrCents, 0)
           ELSE 0 END
  ),0) / 100.0        AS drUnits,
  COALESCE(SUM(
      CASE WHEN lower(t.Status) IN ('jama','credit')
           THEN COALESCE(t.CrCents, t.DrCents, 0)
           ELSE 0 END
  ),0) / 100.0        AS crUnits
FROM AccType at
LEFT JOIN Transactions_P t
  ON t.AccTypeID = at.AccTypeID
  AND t.TDate IS NOT NULL
  AND substr(t.TDate,1,7) = :ym
GROUP BY t.AccTypeID, at.AccTypeName
ORDER BY at.AccTypeName COLLATE NOCASE ASC
""")
    fun currencyTotalsByMonth(ym: String): kotlinx.coroutines.flow.Flow<List<CurrencyTotals>>

    /*
    // Version B (PLAIN SUMS): if you prefer raw columns ignoring Status
    @Query("""
    SELECT
      t.AccTypeID AS accTypeId,
      at.AccTypeName AS name,
      COALESCE(SUM(t.DrCents),0)/100.0 AS drUnits,
      COALESCE(SUM(t.CrCents),0)/100.0 AS crUnits
    FROM AccType at
    LEFT JOIN Transactions_P t
      ON t.AccTypeID = at.AccTypeID
      AND t.TDate IS NOT NULL
      AND substr(t.TDate,1,7) = :ym
    GROUP BY t.AccTypeID, at.AccTypeName
    ORDER BY at.AccTypeName COLLATE NOCASE ASC
    """)
    fun currencyTotalsByMonthPlain(ym: String): Flow<List<CurrencyTotals>>
    */

    data class CurrencyBreak(
        val currency: String,
        val crUnits: Double,
        val drUnits: Double
    )

    @Query("""
    SELECT 
        COALESCE(at.AccTypeName,'')         AS currency,
        COALESCE(SUM(CASE 
            WHEN lower(tp.Status) IN ('jama','credit') 
                 THEN COALESCE(tp.CrCents, tp.DrCents, 0) 
            ELSE 0 END),0) / 100.0          AS crUnits,
        COALESCE(SUM(CASE 
            WHEN lower(tp.Status) IN ('banam','benaam','benam','debit') 
                 THEN COALESCE(tp.DrCents, tp.CrCents, 0) 
            ELSE 0 END),0) / 100.0          AS drUnits
    FROM Transactions_P tp
    LEFT JOIN AccType at ON at.AccTypeID = tp.AccTypeID
    WHERE tp.TDate IS NOT NULL
      AND substr(tp.TDate,1,7) = :ym
    GROUP BY currency
    ORDER BY currency
""")
    fun sumsByCurrency(ym: String): Flow<List<DashboardViewModel.CurrencyBreak>>


    // In TransactionsPDao



    // In TransactionsPDao

    data class TxRow(
        val voucherNo: Long,
        val date: String,
        val name: String,
        val description: String?,
        val drCents: Long,
        val crCents: Long,
        val status: String?,
        val currency: String   // keep this if you’re showing currency
    )

    @Query("""
    SELECT 
        t.VoucherNo              AS voucherNo,
        substr(t.TDate,1,10)     AS date,
        COALESCE(p.Name,'')      AS name,
        t.Description            AS description,
        COALESCE(t.DrCents,0)    AS drCents,
        COALESCE(t.CrCents,0)    AS crCents,
        t.Status                 AS status,
        COALESCE(at.AccTypeName,'') AS currency
    FROM Transactions_P t
    INNER JOIN Acc_Personal p ON p.AccId = t.AccID
    INNER JOIN AccType at      ON at.AccTypeID = t.AccTypeID
    WHERE (:name IS NULL OR :name = '' OR p.Name LIKE '%' || :name || '%')
      AND (
            UPPER(:only) = 'ALL'
         OR (UPPER(:only) = 'DEBIT'  AND COALESCE(t.DrCents,0) > 0)
         OR (UPPER(:only) = 'CREDIT' AND COALESCE(t.CrCents,0) > 0)
      )
      -- 🔽 NEW: optional date bounds in ISO yyyy-MM-dd (inclusive)
      AND (:startDate IS NULL OR substr(t.TDate,1,10) >= :startDate)
      AND (:endDate   IS NULL OR substr(t.TDate,1,10) <= :endDate)
    ORDER BY t.VoucherNo DESC
    LIMIT :limit
""")
    fun lastTransactions(
        limit: Int,
        name: String?,
        only: String,        // pass TxFilter.name
        startDate: String?,  // 🔽 NEW (nullable, ISO "yyyy-MM-dd")
        endDate: String?     // 🔽 NEW (nullable, ISO "yyyy-MM-dd")
    ): Flow<List<TxRow>>




    // In TransactionsPDao.kt

    // --- Balance result row for a searched person (optionally within a date range)
    data class BalanceRow(
        val accId: Long,
        val accTypeId: Long,
        val name: String,
        val accType: String,
        val drUnits: Double,
        val crUnits: Double,
        val balanceUnits: Double
    )

    /**
     * Balance for a searched name.
     * - Only runs when a non-blank name is provided (ViewModel guards this).
     * - Optional date range (inclusive). Pass null to ignore.
     * - Returns the *best* match (first) if multiple names match; adjust ORDER BY if you prefer.
     */
    // ===== Balance (per person) =====
    data class PersonBalanceRow(
        val accId: Long,
        val name: String,
        val currency: String,
        val crCents: Long,
        val drCents: Long
    )

    @Query("""
SELECT 
  t.AccID                           AS accId,
  COALESCE(p.Name,'')               AS name,
  COALESCE(at.AccTypeName,'')       AS currency,
  COALESCE(SUM(CASE 
    WHEN lower(COALESCE(t.Status,'')) IN ('jama','credit') 
      THEN COALESCE(t.CrCents,0) ELSE 0 END),0) AS crCents,
  COALESCE(SUM(CASE 
    WHEN lower(COALESCE(t.Status,'')) IN ('banam','banaam','benaam','benam','debit') 
      THEN COALESCE(t.DrCents,0) ELSE 0 END),0) AS drCents
FROM Transactions_P t
JOIN Acc_Personal p ON p.AccId = t.AccID
LEFT JOIN AccType at ON at.AccTypeID = t.AccTypeID
WHERE (:name IS NOT NULL AND :name <> '' AND p.Name LIKE '%' || :name || '%')
  AND (:start IS NULL OR substr(t.TDate,1,10) >= :start)
  AND (:end   IS NULL OR substr(t.TDate,1,10) <= :end)
GROUP BY t.AccID, p.Name, at.AccTypeName
ORDER BY COUNT(*) DESC      -- if multiple matches, show the most active one
LIMIT 1
""")
    fun personBalance(
        name: String?,
        start: String?,
        end: String?
    ):
            kotlinx.coroutines.flow.Flow<PersonBalanceRow?>


    // --- Per-currency balance rows for a single person (name) ---
    data class BalanceCurrencyRow(
        val currency: String,   // e.g., "PKR", "USD", "AED"
        val creditUnits: Float, // sum of effective credits in UNITS
        val debitUnits: Float   // sum of effective debits  in UNITS
    )

    @Query("""
    SELECT
        COALESCE(at.AccTypeName, '') AS currency,

        /* status-aware CREDIT in UNITS */
        COALESCE(SUM(
            CASE
              WHEN lower(COALESCE(t.Status,'')) IN ('jama','credit')
                   THEN COALESCE(t.CrCents, t.DrCents, 0)
              ELSE 0
            END
        ),0) / 100.0 AS creditUnits,

        /* status-aware DEBIT in UNITS */
        COALESCE(SUM(
            CASE
              WHEN lower(COALESCE(t.Status,'')) IN ('banam','benaam','benam','debit')
                   THEN COALESCE(t.DrCents, t.CrCents, 0)
              ELSE 0
            END
        ),0) / 100.0 AS debitUnits

    FROM Transactions_P t
    INNER JOIN Acc_Personal p ON p.AccId     = t.AccID
    INNER JOIN AccType      at ON at.AccTypeID = t.AccTypeID

    WHERE (:name IS NOT NULL AND :name <> '' AND p.Name LIKE '%' || :name || '%')
      AND (:startDate IS NULL OR substr(t.TDate,1,10) >= :startDate)  -- yyyy-MM-dd
      AND (:endDate   IS NULL OR substr(t.TDate,1,10) <= :endDate)

    GROUP BY at.AccTypeName
    /* keep rows that have any activity */
    HAVING
        COALESCE(SUM(
            CASE WHEN lower(COALESCE(t.Status,'')) IN ('jama','credit')
                 THEN COALESCE(t.CrCents, t.DrCents, 0) ELSE 0 END
        ),0) > 0
        OR
        COALESCE(SUM(
            CASE WHEN lower(COALESCE(t.Status,'')) IN ('banam','benaam','benam','debit')
                 THEN COALESCE(t.DrCents, t.CrCents, 0) ELSE 0 END
        ),0) > 0

    ORDER BY at.AccTypeName COLLATE NOCASE ASC
""")
    fun balanceByCurrencyForName(
        name: String,
        startDate: String?,   // yyyy-MM-dd or null
        endDate: String?      // yyyy-MM-dd or null
    ): kotlinx.coroutines.flow.Flow<List<BalanceCurrencyRow>>
}