package co.luka.domain.recurring

data class ExpensePoint(val occurredAt: Long, val amount: Long)

private const val DAY_MS = 24L * 60L * 60L * 1000L

fun looksRecurring(points: List<ExpensePoint>): Boolean {
    if (points.size < 3) return false
    val sorted = points.sortedBy { it.occurredAt }
    val sample = if (sorted.size > 4) sorted.takeLast(4) else sorted
    if (sample.size < 3) return false
    val intervals = sample.zipWithNext { left, right -> right.occurredAt - left.occurredAt }
    if (intervals.any { gap -> gap !in 28 * DAY_MS..32 * DAY_MS }) return false
    val amounts = sample.map { it.amount }.sorted()
    val typical = amounts[amounts.size / 2]
    if (typical <= 0L) return false
    return sample.all { point ->
        kotlin.math.abs(point.amount - typical).toDouble() / typical.toDouble() < 0.15
    }
}
