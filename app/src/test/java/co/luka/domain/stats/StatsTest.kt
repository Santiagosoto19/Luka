package co.luka.domain.stats

import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.domain.time.bucketsOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val today = LocalDate.of(2026, 5, 10)

    @Test
    fun totalsBalanceAndSavingsRate() {
        val totals = DirectionTotals(income = 1_000_000, expense = 400_000)
        assertEquals(600_000L, totals.balance)
        assertEquals(0.6, Stats.savingsRate(totals)!!, 0.0001)
        assertNull(Stats.savingsRate(DirectionTotals(income = 0, expense = 100)))
    }

    @Test
    fun dailyAverageAndProjectionUseInjectedToday() {
        val month = Periods.resolve(PeriodKind.MONTH, today)
        assertEquals(LocalDate.of(2026, 5, 1), month.start)
        assertEquals(LocalDate.of(2026, 6, 1), month.endExclusive)
        assertEquals(10, Periods.elapsedDays(month, today))
        val average = Stats.dailyAverage(expense = 310_000, elapsedDays = Periods.elapsedDays(month, today))
        assertEquals(31_000L, average)
        assertEquals(31_000L * 31, Stats.monthProjection(average, Periods.daysInMonth(month), Periods.isCurrentMonth(month, today)))
        val closed = Periods.previous(month)
        assertEquals(LocalDate.of(2026, 4, 1), closed.start)
        assertFalse(Periods.isCurrentMonth(closed, today))
        assertNull(Stats.monthProjection(average, Periods.daysInMonth(closed), isCurrentMonth = false))
    }

    @Test
    fun comparisonAvoidsDivideByZero() {
        val up = Stats.deltaPercent(current = 150, previous = 100)
        assertEquals(50.0, up.percent!!, 0.001)
        assertFalse(up.increasedFromZero)

        val fromZero = Stats.deltaPercent(current = 100, previous = 0)
        assertNull(fromZero.percent)
        assertTrue(fromZero.increasedFromZero)

        val down = Stats.deltaPercent(current = 50, previous = 100)
        assertEquals(-50.0, down.percent!!, 0.001)

        val flat = Stats.deltaPercent(current = 0, previous = 0)
        assertEquals(0.0, flat.percent!!, 0.001)
        assertFalse(flat.increasedFromZero)
    }

    @Test
    fun weekStartsMondayAndPreviousIsThePriorWindow() {
        val wednesday = LocalDate.of(2026, 3, 18)
        val week = Periods.resolve(PeriodKind.WEEK, wednesday)
        assertEquals(DayOfWeek.MONDAY, week.start.dayOfWeek)
        assertEquals(7, java.time.temporal.ChronoUnit.DAYS.between(week.start, week.endExclusive))
        assertTrue(!wednesday.isBefore(week.start) && wednesday.isBefore(week.endExclusive))
        val previous = Periods.previous(week)
        assertEquals(week.start.minusWeeks(1), previous.start)
        assertEquals(week.start, previous.endExclusive)
    }

    @Test
    fun largestNamedAmountAndCumulativeBalance() {
        val top = Stats.largest(
            listOf(
                NamedAmount("Rappi", 20_000),
                NamedAmount("Éxito", 80_000, id = 4),
            ),
        )
        assertEquals("Éxito", top?.name)
        assertEquals(80_000L, top?.amount)
        assertEquals(listOf(10L, 5L, 25L), Stats.cumulative(listOf(10, -5, 20)))
    }

    @Test
    fun insightsStayEmptyWithoutDataAndUseRealPercents() {
        assertTrue(
            Stats.insights(
                totals = DirectionTotals(0, 0),
                currentCategories = emptyList(),
                previousCategories = emptyList(),
                subscriptionMonthlyCop = 0,
                expenseProjection = null,
            ).isEmpty(),
        )

        val lines = Stats.insights(
            totals = DirectionTotals(income = 2_000_000, expense = 120_000),
            currentCategories = listOf(NamedAmount("Comida", 120_000)),
            previousCategories = listOf(NamedAmount("Comida", 100_000)),
            subscriptionMonthlyCop = 45_000,
            expenseProjection = 372_000,
        )
        assertTrue(lines.any { it == "Comida subió 20% frente al periodo anterior." })
        assertTrue(lines.any { it.contains("Las suscripciones suman $45.000 al mes.") })
        assertTrue(lines.any { it.contains("ahorro del mes") && it.contains("$") })

        val withoutBaseline = Stats.insights(
            totals = DirectionTotals(income = 0, expense = 50_000),
            currentCategories = listOf(NamedAmount("Comida", 50_000)),
            previousCategories = emptyList(),
            subscriptionMonthlyCop = 0,
            expenseProjection = null,
        )
        assertTrue(withoutBaseline.any { it.contains("Comida") })
        assertTrue(withoutBaseline.none { it.contains("%") })
    }

    @Test
    fun bucketsUseBogotaNotUtc() {
        val zoned = ZonedDateTime.of(2026, 1, 5, 15, 0, 0, 0, BOGOTA)
        val buckets = bucketsOf(zoned.toInstant().toEpochMilli())
        assertEquals("2026-01-05", buckets.occurredDay)
        assertEquals("2026-01", buckets.occurredMonth)
        assertEquals(15, buckets.hour)
        assertEquals(DayOfWeek.MONDAY, zoned.dayOfWeek)
        assertEquals(0, buckets.weekday)
    }

    @Test
    fun heatmapUsesChosenPeriodNotCalendarMonth() {
        val today = LocalDate.of(2026, 5, 10)
        val dayWindow = Periods.resolve(PeriodKind.TODAY, today)
        val dayCells = Periods.heatmap(dayWindow, mapOf(today.toString() to 8_000L))
        assertEquals(1, dayCells.days.size)
        assertEquals(10 to 8_000L, dayCells.days.single())
        assertEquals(today.dayOfWeek.value - 1, dayCells.lead)

        val weekWindow = Periods.resolve(PeriodKind.WEEK, today)
        val weekCells = Periods.heatmap(weekWindow, emptyMap())
        assertEquals(7, weekCells.days.size)
        assertEquals(weekWindow.start.dayOfMonth to 0L, weekCells.days.first())
        assertTrue(weekCells.days.size < today.lengthOfMonth())

        val monthWindow = Periods.resolve(PeriodKind.MONTH, today)
        val monthCells = Periods.heatmap(monthWindow, emptyMap())
        assertEquals(today.lengthOfMonth(), monthCells.days.size)
        assertEquals(1, monthCells.days.first().first)
    }

    @Test
    fun subscriptionCosts() {
        assertEquals(30_000L, Stats.monthlyCost(30_000, "monthly"))
        assertEquals(360_000L, Stats.yearlyCost(30_000, "monthly"))
        assertEquals(10_000L, Stats.monthlyCost(120_000, "yearly"))
        assertEquals(120_000L, Stats.yearlyCost(120_000, "yearly"))
    }
}
