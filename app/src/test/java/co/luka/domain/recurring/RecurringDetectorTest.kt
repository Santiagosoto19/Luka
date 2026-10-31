package co.luka.domain.recurring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringDetectorTest {
    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun suggestsStableMonthlyMerchant() {
        val start = 1_700_000_000_000L
        val points = listOf(
            ExpensePoint(start, 30_000),
            ExpensePoint(start + 30 * day, 31_000),
            ExpensePoint(start + 60 * day, 29_500),
        )
        assertTrue(looksRecurring(points))
    }

    @Test
    fun rejectsUnstableAmountOrShortHistory() {
        val start = 1_700_000_000_000L
        assertFalse(looksRecurring(listOf(ExpensePoint(start, 30_000), ExpensePoint(start + 30 * day, 30_000))))
        assertFalse(
            looksRecurring(
                listOf(
                    ExpensePoint(start, 10_000),
                    ExpensePoint(start + 30 * day, 30_000),
                    ExpensePoint(start + 60 * day, 30_000),
                ),
            ),
        )
    }
}
