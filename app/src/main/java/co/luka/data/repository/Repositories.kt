package co.luka.data.repository

import co.luka.data.alerts.BudgetWatcher
import co.luka.data.db.BudgetEntity
import co.luka.data.db.CategoryEntity
import co.luka.data.db.GoalContributionEntity
import co.luka.data.db.GoalEntity
import co.luka.data.db.LukaDatabase
import co.luka.data.db.MerchantEntity
import co.luka.data.db.MovementEntity
import co.luka.data.db.SourceEntity
import co.luka.data.db.SubscriptionEntity
import co.luka.data.db.newMovement
import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.model.Direction
import co.luka.domain.model.incomeKindFor
import co.luka.domain.recurring.ExpensePoint
import co.luka.domain.recurring.looksRecurring
import co.luka.domain.time.bucketsOf
import co.luka.domain.wallet.WalletMath
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

data class ManualEntry(
    val amount: Long,
    val direction: Direction,
    val categoryId: Long?,
    val note: String?,
    val counterparty: String?,
    val occurredAt: Long,
    val sourceId: Long?,
    val counterSourceId: Long? = null,
)

@Singleton
class MovementRepository @Inject constructor(
    private val database: LukaDatabase,
    private val budgets: BudgetWatcher,
) {
    fun observeRecent(limit: Int = 8) = database.movementDao().observeRecent(limit)

    suspend fun recentInRange(start: String, endExclusive: String, sourceId: Long?, limit: Int) =
        database.movementDao().recentInRange(start, endExclusive, sourceId, limit)

    fun observeFiltered(
        fromDay: String?,
        toDay: String?,
        categoryId: Long?,
        sourceId: Long?,
        direction: String?,
        minAmount: Long?,
        maxAmount: Long?,
    ) = database.movementDao().observeFiltered(fromDay, toDay, categoryId, sourceId, direction, minAmount, maxAmount)

    suspend fun cashSourceId(): Long? = database.sourceDao().cash()?.id

    suspend fun insertManual(entry: ManualEntry): Long {
        require(entry.amount > 0L) { "El monto tiene que ser mayor que cero." }
        val transfer = entry.direction == Direction.TRANSFER
        if (transfer) requireDistinctWallets(entry.sourceId, entry.counterSourceId)
        val categoryId = if (transfer) null else entry.categoryId
        val categoryName = categoryId?.let { database.categoryDao().find(it)?.name }
        val merchantId = entry.counterparty?.takeIf { it.isNotBlank() }?.let { name ->
            rememberMerchant(name, categoryId, learnedFrom = if (categoryId != null) "user" else "auto")
        }
        val id = database.movementDao().insert(
            newMovement(
                direction = entry.direction.wire,
                amount = entry.amount,
                occurredAt = entry.occurredAt,
                categoryId = categoryId,
                merchantId = merchantId,
                counterparty = entry.counterparty?.trim()?.takeIf { it.isNotEmpty() },
                note = entry.note?.trim()?.takeIf { it.isNotEmpty() },
                sourceId = if (transfer) entry.sourceId else entry.sourceId ?: database.sourceDao().cash()?.id,
                incomeKind = incomeKindFor(entry.direction, categoryName),
                origin = "manual",
                status = "confirmed",
                captureId = null,
                dedupeKey = null,
                counterSourceId = if (transfer) entry.counterSourceId else null,
            ),
        )
        if (entry.direction == Direction.EXPENSE) budgets.onExpense(categoryId, entry.occurredAt)
        return id
    }

    suspend fun update(id: Long, entry: ManualEntry) {
        require(entry.amount > 0L) { "El monto tiene que ser mayor que cero." }
        val current = database.movementDao().find(id) ?: return
        val transfer = entry.direction == Direction.TRANSFER
        val sourceId = entry.sourceId ?: current.sourceId
        val counterSourceId = if (transfer) entry.counterSourceId else null
        if (transfer) requireDistinctWallets(sourceId, counterSourceId)
        val categoryId = if (transfer) null else entry.categoryId
        val categoryName = categoryId?.let { database.categoryDao().find(it)?.name }
        val party = entry.counterparty?.trim()?.takeIf { it.isNotEmpty() } ?: current.counterparty
        val merchantId = if (party != null && categoryId != null) {
            rememberMerchant(party, categoryId, learnedFrom = "user")
        } else if (transfer) {
            null
        } else {
            current.merchantId
        }
        val fresh = newMovement(
            direction = entry.direction.wire,
            amount = entry.amount,
            occurredAt = entry.occurredAt,
            categoryId = categoryId,
            merchantId = merchantId,
            counterparty = party,
            note = entry.note?.trim()?.takeIf { it.isNotEmpty() },
            sourceId = sourceId,
            incomeKind = incomeKindFor(entry.direction, categoryName),
            origin = current.origin,
            status = "confirmed",
            captureId = current.captureId,
            dedupeKey = current.dedupeKey,
            now = current.createdAt,
            counterSourceId = counterSourceId,
        )
        database.movementDao().update(fresh.copy(id = id, updatedAt = System.currentTimeMillis()))
        if (entry.direction == Direction.EXPENSE && current.origin != "adjustment") {
            budgets.onExpense(categoryId, entry.occurredAt)
        }
    }

    suspend fun balances(): Map<Long, Long> {
        val parts = database.movementDao().balanceParts().associateBy { it.sourceId }
        return database.sourceDao().all().associate { source ->
            val part = parts[source.id]
            source.id to WalletMath.balance(
                opening = source.openingBalance,
                income = part?.income ?: 0L,
                expense = part?.expense ?: 0L,
                adjustmentIn = part?.adjustmentIn ?: 0L,
                adjustmentOut = part?.adjustmentOut ?: 0L,
                transferOut = part?.transferOut ?: 0L,
                transferIn = part?.transferIn ?: 0L,
            )
        }
    }

    suspend fun reconcile(sourceId: Long, realAmount: Long): Boolean {
        require(realAmount >= 0L) { "El monto tiene que ser cero o mayor." }
        val delta = realAmount - (balances()[sourceId] ?: 0L)
        if (delta == 0L) return true
        database.movementDao().insert(
            newMovement(
                direction = if (delta > 0L) Direction.INCOME.wire else Direction.EXPENSE.wire,
                amount = abs(delta),
                occurredAt = System.currentTimeMillis(),
                categoryId = null,
                merchantId = null,
                counterparty = null,
                note = "Ajuste para cuadrar",
                sourceId = sourceId,
                incomeKind = null,
                origin = "adjustment",
                status = "confirmed",
                captureId = null,
                dedupeKey = null,
            ),
        )
        return false
    }

    suspend fun softDelete(id: Long) {
        database.movementDao().softDelete(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        database.movementDao().restore(id, System.currentTimeMillis())
    }

    suspend fun find(id: Long): MovementEntity? = database.movementDao().find(id)

    private fun requireDistinctWallets(fromId: Long?, toId: Long?) {
        require(fromId != null && toId != null && fromId != toId) { "Elige dos carteras distintas." }
    }

    private suspend fun rememberMerchant(displayName: String, categoryId: Long?, learnedFrom: String): Long {
        val normalized = normalizeMerchant(displayName)
        val now = System.currentTimeMillis()
        val existing = database.merchantDao().findByName(normalized)
        if (existing == null) {
            return database.merchantDao().insert(
                MerchantEntity(
                    normalizedName = normalized,
                    displayName = displayName.trim(),
                    defaultCategoryId = categoryId,
                    learnedFrom = learnedFrom,
                    confirmations = 1,
                    updatedAt = now,
                ),
            )
        }
        val nextCategory = if (learnedFrom == "user") categoryId else existing.defaultCategoryId
        val nextLearned = if (learnedFrom == "user") "user" else existing.learnedFrom
        database.merchantDao().update(
            existing.copy(
                displayName = displayName.trim(),
                defaultCategoryId = nextCategory,
                learnedFrom = nextLearned,
                confirmations = existing.confirmations + 1,
                updatedAt = now,
            ),
        )
        return existing.id
    }
}

@Singleton
class CatalogRepository @Inject constructor(
    private val database: LukaDatabase,
) {
    fun observeCategories(includeArchived: Boolean): Flow<List<CategoryEntity>> =
        if (includeArchived) database.categoryDao().observeAll() else database.categoryDao().observeActive()

    fun observeSources(): Flow<List<SourceEntity>> = database.sourceDao().observeAll()

    suspend fun saveCategory(name: String, direction: String, color: String, icon: String): Long {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "La categoría necesita un nombre." }
        val order = database.categoryDao().all().size
        return database.categoryDao().insert(
            CategoryEntity(
                name = clean,
                direction = direction,
                icon = icon,
                color = color,
                sortOrder = order,
                isSystem = false,
                archived = false,
            ),
        )
    }

    suspend fun updateCategory(category: CategoryEntity) {
        database.categoryDao().update(category)
    }

    suspend fun updateSource(source: SourceEntity) {
        database.sourceDao().update(source)
    }

    suspend fun setOpeningBalance(sourceId: Long, amount: Long) {
        require(amount >= 0L) { "El saldo inicial no puede ser negativo." }
        val source = database.sourceDao().find(sourceId) ?: return
        database.sourceDao().update(source.copy(openingBalance = amount))
    }
}

@Singleton
class PlanningRepository @Inject constructor(
    private val database: LukaDatabase,
) {
    fun observeGoals() = database.goalDao().observeProgress()
    fun observeSubscriptions() = database.subscriptionDao().observeAll()
    fun budgetRevision() = database.budgetDao().revision()

    suspend fun budgets(month: String) = database.budgetDao().progress(month)

    suspend fun saveBudget(categoryId: Long, limitAmount: Long, alertRatio: Double) {
        require(limitAmount > 0L) { "El límite tiene que ser mayor que cero." }
        val ratio = alertRatio.coerceIn(0.1, 1.0)
        val existing = database.budgetDao().find(categoryId, "month")
        if (existing == null) {
            database.budgetDao().insert(
                BudgetEntity(
                    categoryId = categoryId,
                    limitAmount = limitAmount,
                    period = "month",
                    alertRatio = ratio,
                    active = true,
                ),
            )
        } else {
            database.budgetDao().update(existing.copy(limitAmount = limitAmount, alertRatio = ratio, active = true))
        }
    }

    suspend fun setBudgetActive(id: Long, active: Boolean) {
        val budget = database.budgetDao().all().firstOrNull { it.id == id } ?: return
        database.budgetDao().update(budget.copy(active = active))
    }

    suspend fun saveGoal(name: String, targetAmount: Long, deadlineDay: String?, color: String, icon: String): Long {
        require(name.isNotBlank()) { "La meta necesita un nombre." }
        require(targetAmount > 0L) { "El monto de la meta tiene que ser mayor que cero." }
        return database.goalDao().insert(
            GoalEntity(
                name = name.trim(),
                targetAmount = targetAmount,
                deadlineDay = deadlineDay,
                icon = icon,
                color = color,
                archived = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun archiveGoal(id: Long) {
        val goal = database.goalDao().all().firstOrNull { it.id == id } ?: return
        database.goalDao().update(goal.copy(archived = true))
    }

    suspend fun contribute(goalId: Long, amount: Long, note: String?) {
        require(amount != 0L) { "El aporte no puede ser cero." }
        database.goalDao().insertContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = amount,
                occurredAt = System.currentTimeMillis(),
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun refreshSuggestions() {
        val grouped = database.movementDao().recurringCandidates().groupBy { it.merchantId }
        val now = System.currentTimeMillis()
        grouped.forEach { (merchantId, rows) ->
            val points = rows.map { ExpensePoint(it.occurredAt, it.amount) }
            if (!looksRecurring(points)) return@forEach
            if (database.subscriptionDao().findByMerchant(merchantId) != null) return@forEach
            val typical = rows.map { it.amount }.sorted().let { it[it.size / 2] }
            val last = rows.maxOf { it.occurredAt }
            database.subscriptionDao().insert(
                SubscriptionEntity(
                    merchantId = merchantId,
                    categoryId = null,
                    displayName = rows.last().displayName,
                    typicalAmount = typical,
                    cadence = "monthly",
                    status = "suggested",
                    lastSeenAt = last,
                    nextExpectedAt = last + 30L * 24L * 60L * 60L * 1000L,
                    createdAt = now,
                ),
            )
        }
    }

    suspend fun setSubscriptionStatus(id: Long, status: String) {
        val item = database.subscriptionDao().all().firstOrNull { it.id == id } ?: return
        val next = if (status == "active") {
            (item.lastSeenAt ?: System.currentTimeMillis()) + 30L * 24L * 60L * 60L * 1000L
        } else {
            item.nextExpectedAt
        }
        database.subscriptionDao().update(item.copy(status = status, nextExpectedAt = next))
    }
}

@Singleton
class CaptureRepository @Inject constructor(
    private val database: LukaDatabase,
    private val movements: MovementRepository,
) {
    fun observeReview() = database.captureDao().observeReview()

    suspend fun complete(captureId: Long, entry: ManualEntry) {
        val capture = database.captureDao().find(captureId) ?: return
        val movementId = movements.insertManual(entry.copy(sourceId = entry.sourceId ?: capture.sourceId))
        database.captureDao().link(captureId, movementId, "parsed")
        val movement = database.movementDao().find(movementId) ?: return
        database.movementDao().update(movement.copy(captureId = captureId, origin = capture.channel))
    }

    suspend fun ignore(captureId: Long) {
        database.captureDao().link(captureId, null, "ignored")
    }
}

@Singleton
class StatsRepository @Inject constructor(
    private val database: LukaDatabase,
) {
    fun revision() = database.movementDao().revision()

    suspend fun totalsFor(start: String, endExclusive: String, sourceId: Long?) =
        database.movementDao().sumsByDirectionFiltered(start, endExclusive, sourceId)

    suspend fun daysFor(start: String, endExclusive: String, sourceId: Long?) =
        database.movementDao().sumsByDayFiltered(start, endExclusive, sourceId)

    suspend fun balancesBySource(start: String, endExclusive: String) =
        database.movementDao().sumsBySourceAndDirection(start, endExclusive)

    suspend fun expenseBetween(start: String, endExclusive: String) =
        database.movementDao().expenseByDay(start, endExclusive)

    suspend fun load(start: String, endExclusive: String, fromMonth: String, budgetMonth: String) = StatsLoad(
        totals = database.movementDao().sumsByDirection(start, endExclusive),
        categories = database.movementDao().sumsByCategory("expense", start, endExclusive),
        incomeCategories = database.movementDao().sumsByCategory("income", start, endExclusive),
        months = database.movementDao().sumsByMonth(fromMonth),
        days = database.movementDao().sumsByDay(start, endExclusive),
        merchants = database.movementDao().topMerchants(start, endExclusive),
        sources = database.movementDao().sumsBySource(start, endExclusive),
        hours = database.movementDao().sumsByHour(start, endExclusive),
        weekdays = database.movementDao().sumsByWeekday(start, endExclusive),
        incomeKinds = database.movementDao().sumsByIncomeKind(start, endExclusive),
        topExpense = database.movementDao().topExpense(start, endExclusive),
        budgets = database.budgetDao().progress(budgetMonth),
        goals = database.goalDao().progress(),
        subscriptions = database.subscriptionDao().relevant(),
        expenseDays = database.movementDao().expenseByDay(start, endExclusive),
    )
}

data class StatsLoad(
    val totals: List<co.luka.data.db.DirectionSum>,
    val categories: List<co.luka.data.db.CategoryTotal>,
    val incomeCategories: List<co.luka.data.db.CategoryTotal>,
    val months: List<co.luka.data.db.MonthSum>,
    val days: List<co.luka.data.db.DaySum>,
    val merchants: List<co.luka.data.db.MerchantSum>,
    val sources: List<co.luka.data.db.SourceTotal>,
    val hours: List<co.luka.data.db.HourSum>,
    val weekdays: List<co.luka.data.db.WeekdaySum>,
    val incomeKinds: List<co.luka.data.db.IncomeKindSum>,
    val topExpense: co.luka.data.db.TopExpense?,
    val budgets: List<co.luka.data.db.BudgetProgress>,
    val goals: List<co.luka.data.db.GoalProgress>,
    val subscriptions: List<SubscriptionEntity>,
    val expenseDays: List<co.luka.data.db.DayExpense>,
)
