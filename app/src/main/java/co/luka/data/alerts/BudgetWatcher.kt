package co.luka.data.alerts

import co.luka.data.db.LukaDatabase
import co.luka.data.settings.SettingsStore
import co.luka.domain.time.bucketsOf
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class BudgetWatcher @Inject constructor(
    private val database: LukaDatabase,
    private val settings: SettingsStore,
    private val alerts: LocalAlerts,
) {
    suspend fun onExpense(categoryId: Long?, occurredAt: Long) {
        if (categoryId == null) return
        val month = bucketsOf(occurredAt).occurredMonth
        val budget = database.budgetDao().findActive(categoryId, "month") ?: return
        val spent = database.movementDao().sumCategoryMonth(categoryId, month)
        // El monto vive en Long. El ratio solo sirve para el umbral de aviso.
        val threshold = (budget.limitAmount.toDouble() * budget.alertRatio).roundToLong()
        if (spent < threshold) return
        val key = "${budget.id}-$month"
        if (settings.hasBudgetAlert(key)) return
        settings.rememberBudgetAlert(key)
        val name = database.categoryDao().find(categoryId)?.name ?: "la categoría"
        alerts.budget(name, spent, budget.limitAmount)
    }
}
