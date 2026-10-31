package co.luka.domain.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

val BOGOTA: ZoneId = ZoneId.of("America/Bogota")

data class TimeBuckets(
    val occurredAt: Long,
    val occurredDay: String,
    val occurredMonth: String,
    val weekday: Int,
    val hour: Int,
)

fun bucketsOf(epochMillis: Long): TimeBuckets {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(BOGOTA)
    val date = dateTime.toLocalDate()
    return TimeBuckets(
        occurredAt = epochMillis,
        occurredDay = date.toString(),
        occurredMonth = "%04d-%02d".format(date.year, date.monthValue),
        weekday = dateTime.dayOfWeek.value - 1,
        hour = dateTime.hour,
    )
}

enum class PeriodKind {
    TODAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    CUSTOM,
}

data class PeriodWindow(
    val kind: PeriodKind,
    val start: LocalDate,
    val endExclusive: LocalDate,
)

data class HeatmapCells(
    val lead: Int,
    val days: List<Pair<Int, Long>>,
)

object Periods {
    fun resolve(
        kind: PeriodKind,
        anchor: LocalDate,
        customStart: LocalDate? = null,
        customEndInclusive: LocalDate? = null,
    ): PeriodWindow = when (kind) {
        PeriodKind.TODAY -> PeriodWindow(kind, anchor, anchor.plusDays(1))
        PeriodKind.WEEK -> {
            val start = mondayOf(anchor)
            PeriodWindow(kind, start, start.plusWeeks(1))
        }
        PeriodKind.MONTH -> {
            val start = anchor.withDayOfMonth(1)
            PeriodWindow(kind, start, start.plusMonths(1))
        }
        PeriodKind.QUARTER -> {
            val startMonth = ((anchor.monthValue - 1) / 3) * 3 + 1
            val start = LocalDate.of(anchor.year, startMonth, 1)
            PeriodWindow(kind, start, start.plusMonths(3))
        }
        PeriodKind.YEAR -> {
            val start = LocalDate.of(anchor.year, 1, 1)
            PeriodWindow(kind, start, start.plusYears(1))
        }
        PeriodKind.CUSTOM -> {
            val start = customStart ?: anchor
            val inclusiveEnd = customEndInclusive ?: anchor
            val end = inclusiveEnd.plusDays(1)
            val safeEnd = if (!end.isAfter(start)) start.plusDays(1) else end
            PeriodWindow(kind, start, safeEnd)
        }
    }

    fun previous(window: PeriodWindow): PeriodWindow = shift(window, -1)

    fun shift(window: PeriodWindow, steps: Int): PeriodWindow {
        if (steps == 0) return window
        return when (window.kind) {
            PeriodKind.TODAY -> window.copy(
                start = window.start.plusDays(steps.toLong()),
                endExclusive = window.endExclusive.plusDays(steps.toLong()),
            )
            PeriodKind.WEEK -> window.copy(
                start = window.start.plusWeeks(steps.toLong()),
                endExclusive = window.endExclusive.plusWeeks(steps.toLong()),
            )
            PeriodKind.MONTH -> {
                val start = window.start.plusMonths(steps.toLong())
                PeriodWindow(window.kind, start, start.plusMonths(1))
            }
            PeriodKind.QUARTER -> {
                val start = window.start.plusMonths(3L * steps)
                PeriodWindow(window.kind, start, start.plusMonths(3))
            }
            PeriodKind.YEAR -> {
                val start = window.start.plusYears(steps.toLong())
                PeriodWindow(window.kind, start, start.plusYears(1))
            }
            PeriodKind.CUSTOM -> {
                val length = ChronoUnit.DAYS.between(window.start, window.endExclusive)
                val start = window.start.plusDays(length * steps)
                PeriodWindow(window.kind, start, start.plusDays(length))
            }
        }
    }

    fun elapsedDays(window: PeriodWindow, today: LocalDate): Int {
        if (today.isBefore(window.start)) return 1
        val last = window.endExclusive.minusDays(1)
        val end = if (today.isAfter(last)) last else today
        return max(1, ChronoUnit.DAYS.between(window.start, end).toInt() + 1)
    }

    fun isCurrentMonth(window: PeriodWindow, today: LocalDate): Boolean =
        window.kind == PeriodKind.MONTH &&
            today.year == window.start.year &&
            today.month == window.start.month &&
            !today.isBefore(window.start) &&
            today.isBefore(window.endExclusive)

    fun daysInMonth(window: PeriodWindow): Int = window.start.lengthOfMonth()

    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )
    private val shortMonths = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    )

    fun shortMonth(date: LocalDate): String = shortMonths[date.monthValue - 1]

    fun label(window: PeriodWindow): String = when (window.kind) {
        PeriodKind.TODAY -> "${window.start.dayOfMonth} de ${monthName(window.start)}"
        PeriodKind.WEEK -> weekLabel(window.start, window.endExclusive.minusDays(1))
        PeriodKind.MONTH -> "${monthTitle(window.start)} ${window.start.year}"
        PeriodKind.QUARTER -> {
            val end = window.endExclusive.minusDays(1)
            "${monthTitle(window.start)} – ${monthName(end)} ${end.year}"
        }
        PeriodKind.YEAR -> window.start.year.toString()
        PeriodKind.CUSTOM -> weekLabel(window.start, window.endExclusive.minusDays(1))
    }

    private fun weekLabel(start: LocalDate, end: LocalDate): String = when {
        start.month == end.month && start.year == end.year ->
            "${start.dayOfMonth} – ${end.dayOfMonth} de ${monthName(start)}"
        else -> "${start.dayOfMonth} ${shortMonth(start)} – ${end.dayOfMonth} ${shortMonth(end)}"
    }

    private fun monthName(date: LocalDate): String = months[date.monthValue - 1]

    private fun monthTitle(date: LocalDate): String =
        monthName(date).replaceFirstChar { it.titlecase(Locale("es", "CO")) }

    fun heatmap(window: PeriodWindow, expenseByDay: Map<String, Long>): HeatmapCells {
        val count = ChronoUnit.DAYS.between(window.start, window.endExclusive).toInt().coerceAtLeast(1)
        val days = (0 until count).map { offset ->
            val date = window.start.plusDays(offset.toLong())
            date.dayOfMonth to (expenseByDay[date.toString()] ?: 0L)
        }
        return HeatmapCells(lead = window.start.dayOfWeek.value - 1, days = days)
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
}
