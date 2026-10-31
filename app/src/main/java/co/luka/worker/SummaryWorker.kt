package co.luka.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.luka.data.alerts.LocalAlerts
import co.luka.data.db.LukaDatabase
import co.luka.domain.money.CopMoney
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: LukaDatabase,
    private val alerts: LocalAlerts,
    private val scheduler: SummaryScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = inputData.getString(SummaryScheduler.KEY_KIND) ?: SummaryScheduler.WEEKLY
        val today = LocalDate.now(BOGOTA)
        val current = if (kind == SummaryScheduler.MONTHLY) {
            Periods.resolve(PeriodKind.MONTH, today)
        } else {
            Periods.resolve(PeriodKind.WEEK, today)
        }
        val window = Periods.previous(current)
        val sums = database.movementDao().sumsByDirection(window.start.toString(), window.endExclusive.toString())
        val income = sums.firstOrNull { it.direction == "income" }?.total ?: 0L
        val expense = sums.firstOrNull { it.direction == "expense" }?.total ?: 0L
        val title = if (kind == SummaryScheduler.MONTHLY) "Resumen del mes" else "Resumen de la semana"
        alerts.summary(
            id = if (kind == SummaryScheduler.MONTHLY) 2 else 1,
            title = title,
            body = "Ingreso ${CopMoney.format(income)}, gasto ${CopMoney.format(expense)}, balance ${CopMoney.format(income - expense)}.",
        )
        scheduler.scheduleNext(kind)
        return Result.success()
    }
}
