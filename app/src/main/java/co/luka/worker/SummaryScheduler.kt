package co.luka.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.luka.domain.time.BOGOTA
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureScheduled() {
        enqueue(WEEKLY, ExistingWorkPolicy.KEEP)
        enqueue(MONTHLY, ExistingWorkPolicy.KEEP)
    }

    fun scheduleNext(kind: String) {
        enqueue(kind, ExistingWorkPolicy.APPEND)
    }

    private fun enqueue(kind: String, policy: ExistingWorkPolicy) {
        val delay = if (kind == MONTHLY) delayUntil(nextMonthStart()) else delayUntil(nextMonday())
        val request = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to kind))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(kind), policy, request)
    }

    private fun name(kind: String) = if (kind == MONTHLY) "luka-monthly" else "luka-weekly"

    private fun delayUntil(target: ZonedDateTime): Long =
        Duration.between(ZonedDateTime.now(BOGOTA), target).toMillis().coerceAtLeast(60_000L)

    private fun nextMonday(): ZonedDateTime {
        val now = ZonedDateTime.now(BOGOTA)
        var candidate = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return candidate
    }

    private fun nextMonthStart(): ZonedDateTime {
        val now = ZonedDateTime.now(BOGOTA)
        var candidate = now.withDayOfMonth(1).withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (!candidate.isAfter(now)) candidate = candidate.plusMonths(1)
        return candidate
    }

    companion object {
        const val KEY_KIND = "kind"
        const val WEEKLY = "weekly"
        const val MONTHLY = "monthly"
    }
}
