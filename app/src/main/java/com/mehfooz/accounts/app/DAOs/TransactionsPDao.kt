package com.mehfooz.accounts.app.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyDebitsCredits(
    val day: Int,
    val drUnits: Float,
    val crUnits: Float
)

data class MonthTotals(
    val drUnits: Double,
    val crUnits: Double
)

@Dao
interface TransactionsPDao {

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

    /* --- KEY CHANGE: sum amounts by Status --- */
    @Query("""
        SELECT CAST(substr(TDate, 9, 2) AS INTEGER) AS day,
               -- debit units
               COALESCE(SUM(
                   CASE
                     WHEN lower(Status) IN ('banam','benaam','benam','debit') THEN
                          COALESCE(DrCents, CrCents, 0)
                     ELSE 0
                   END
               ),0) / 100.0 AS drUnits,
               -- credit units
               COALESCE(SUM(
                   CASE
                     WHEN lower(Status) IN ('jama','credit') THEN
                          COALESCE(CrCents, DrCents, 0)
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
                    WHEN lower(Status) IN ('banam','benaam','benam','debit') THEN
                         COALESCE(DrCents, CrCents, 0)
                    ELSE 0
                END
            ),0) / 100.0 AS drUnits,
            COALESCE(SUM(
                CASE
                    WHEN lower(Status) IN ('jama','credit') THEN
                         COALESCE(CrCents, DrCents, 0)
                    ELSE 0
                END
            ),0) / 100.0 AS crUnits
        FROM Transactions_P
        WHERE TDate IS NOT NULL
          AND substr(TDate,1,7) = :ym
    """)
    fun monthTotals(ym: String): Flow<MonthTotals>

    @Query("""
        SELECT substr(TDate, 1, 10) AS day,
               -- net cents = credits - debits based on Status
               COALESCE(SUM(
                   CASE
                     WHEN lower(Status) IN ('jama','credit') THEN COALESCE(CrCents, DrCents, 0)
                     WHEN lower(Status) IN ('banam','benaam','benam','debit') THEN -COALESCE(DrCents, CrCents, 0)
                     ELSE 0
                   END
               ),0) AS netCents
        FROM Transactions_P
        WHERE substr(TDate,1,7) = :ym
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailyTotalsByYearMonth(ym: String): Flow<List<DailyTotal>>

    data class DayTotals(
        val crUnits: Float,
        val drUnits: Float
    )

    @Query("""
    SELECT
      COALESCE(SUM(CASE WHEN lower(Status)='jama'   THEN CrCents ELSE 0 END),0)/100.0 AS crUnits,
      COALESCE(SUM(CASE WHEN lower(Status)='banaam' THEN DrCents ELSE 0 END),0)/100.0 AS drUnits
    FROM Transactions_P
    WHERE TDate IS NOT NULL AND substr(TDate,1,10) = :date /* yyyy-MM-dd */
""")
    suspend fun totalsForDate(date: String): DayTotals
}