package co.luka.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import co.luka.data.db.BudgetEntity
import co.luka.data.db.eraseAll
import co.luka.data.db.CategoryEntity
import co.luka.data.db.GoalContributionEntity
import co.luka.data.db.GoalEntity
import co.luka.data.db.LukaDatabase
import co.luka.data.db.MerchantEntity
import co.luka.data.db.Seed
import co.luka.data.db.SubscriptionEntity
import co.luka.data.db.newMovement
import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.security.PhraseCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val version: Int = 1,
    val categories: List<BackupCategory> = emptyList(),
    val movements: List<BackupMovement> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val goals: List<BackupGoal> = emptyList(),
    val subscriptions: List<BackupSubscription> = emptyList(),
)

@Serializable
data class BackupCategory(val name: String, val direction: String, val icon: String, val color: String, val sortOrder: Int, val isSystem: Boolean, val archived: Boolean)

@Serializable
data class BackupMovement(
    val direction: String,
    val amount: Long,
    val occurredAt: Long,
    val categoryName: String?,
    val merchantName: String?,
    val counterparty: String?,
    val note: String?,
    val sourceSlug: String?,
    val incomeKind: String?,
    val origin: String,
    val status: String,
)

@Serializable
data class BackupBudget(val categoryName: String, val limitAmount: Long, val period: String, val alertRatio: Double, val active: Boolean)

@Serializable
data class BackupGoal(
    val name: String,
    val targetAmount: Long,
    val deadlineDay: String?,
    val icon: String,
    val color: String,
    val archived: Boolean,
    val createdAt: Long,
    val contributions: List<BackupContribution> = emptyList(),
)

@Serializable
data class BackupContribution(val amount: Long, val occurredAt: Long, val note: String?)

@Serializable
data class BackupSubscription(
    val displayName: String,
    val merchantName: String?,
    val categoryName: String?,
    val typicalAmount: Long,
    val cadence: String,
    val status: String,
    val lastSeenAt: Long?,
    val nextExpectedAt: Long?,
    val createdAt: Long,
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LukaDatabase,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun export(uri: Uri, phrase: CharArray) {
        val categories = database.categoryDao().all()
        val categoryById = categories.associateBy { it.id }
        val merchants = database.merchantDao().all().associateBy { it.id }
        val sources = database.sourceDao().all().associateBy { it.id }
        val contributions = database.goalDao().contributions().groupBy { it.goalId }
        val file = BackupFile(
            categories = categories.map {
                BackupCategory(it.name, it.direction, it.icon, it.color, it.sortOrder, it.isSystem, it.archived)
            },
            movements = database.movementDao().allActive().map { movement ->
                BackupMovement(
                    direction = movement.direction,
                    amount = movement.amount,
                    occurredAt = movement.occurredAt,
                    categoryName = movement.categoryId?.let { categoryById[it]?.name },
                    merchantName = movement.merchantId?.let { merchants[it]?.displayName },
                    counterparty = movement.counterparty,
                    note = movement.note,
                    sourceSlug = movement.sourceId?.let { sources[it]?.slug },
                    incomeKind = movement.incomeKind,
                    origin = movement.origin,
                    status = movement.status,
                )
            },
            budgets = database.budgetDao().all().mapNotNull { budget ->
                val name = categoryById[budget.categoryId]?.name ?: return@mapNotNull null
                BackupBudget(name, budget.limitAmount, budget.period, budget.alertRatio, budget.active)
            },
            goals = database.goalDao().all().map { goal ->
                BackupGoal(
                    name = goal.name,
                    targetAmount = goal.targetAmount,
                    deadlineDay = goal.deadlineDay,
                    icon = goal.icon,
                    color = goal.color,
                    archived = goal.archived,
                    createdAt = goal.createdAt,
                    contributions = contributions[goal.id].orEmpty().map {
                        BackupContribution(it.amount, it.occurredAt, it.note)
                    },
                )
            },
            subscriptions = database.subscriptionDao().all().map { item ->
                BackupSubscription(
                    displayName = item.displayName,
                    merchantName = item.merchantId?.let { merchants[it]?.displayName },
                    categoryName = item.categoryId?.let { categoryById[it]?.name },
                    typicalAmount = item.typicalAmount,
                    cadence = item.cadence,
                    status = item.status,
                    lastSeenAt = item.lastSeenAt,
                    nextExpectedAt = item.nextExpectedAt,
                    createdAt = item.createdAt,
                )
            },
        )
        val bytes = PhraseCipher.encrypt(json.encodeToString(file).toByteArray(Charsets.UTF_8), phrase)
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("No se pudo crear el archivo.")
    }

    suspend fun import(uri: Uri, phrase: CharArray) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer el archivo.")
        val plain = PhraseCipher.decrypt(bytes, phrase)
        val file = json.decodeFromString<BackupFile>(plain.toString(Charsets.UTF_8))
        database.withTransaction {
            database.eraseAll()
            database.categoryDao().insertAll(
                file.categories.map {
                    CategoryEntity(
                        name = it.name,
                        direction = it.direction,
                        icon = it.icon,
                        color = it.color,
                        sortOrder = it.sortOrder,
                        isSystem = it.isSystem,
                        archived = it.archived,
                    )
                },
            )
            database.sourceDao().insertAll(Seed.sources())
            val categories = database.categoryDao().all().associateBy { it.name }
            val sources = database.sourceDao().all().associateBy { it.slug }
            file.movements.forEach { movement ->
                val merchantId = movement.merchantName?.let { name ->
                    val normalized = normalizeMerchant(name)
                    database.merchantDao().findByName(normalized)?.id ?: database.merchantDao().insert(
                        MerchantEntity(
                            normalizedName = normalized,
                            displayName = name,
                            defaultCategoryId = movement.categoryName?.let { categories[it]?.id },
                            learnedFrom = "auto",
                            confirmations = 1,
                            updatedAt = movement.occurredAt,
                        ),
                    )
                }
                database.movementDao().insert(
                    newMovement(
                        direction = movement.direction,
                        amount = movement.amount,
                        occurredAt = movement.occurredAt,
                        categoryId = movement.categoryName?.let { categories[it]?.id },
                        merchantId = merchantId,
                        counterparty = movement.counterparty,
                        note = movement.note,
                        sourceId = movement.sourceSlug?.let { sources[it]?.id } ?: sources["cash"]?.id,
                        incomeKind = movement.incomeKind,
                        origin = movement.origin,
                        status = movement.status,
                        captureId = null,
                        dedupeKey = null,
                    ),
                )
            }
            file.budgets.forEach { budget ->
                val categoryId = categories[budget.categoryName]?.id ?: return@forEach
                database.budgetDao().insert(
                    BudgetEntity(categoryId = categoryId, limitAmount = budget.limitAmount, period = budget.period, alertRatio = budget.alertRatio, active = budget.active),
                )
            }
            file.goals.forEach { goal ->
                val goalId = database.goalDao().insert(
                    GoalEntity(
                        name = goal.name,
                        targetAmount = goal.targetAmount,
                        deadlineDay = goal.deadlineDay,
                        icon = goal.icon,
                        color = goal.color,
                        archived = goal.archived,
                        createdAt = goal.createdAt,
                    ),
                )
                goal.contributions.forEach { contribution ->
                    database.goalDao().insertContribution(
                        GoalContributionEntity(goalId = goalId, amount = contribution.amount, occurredAt = contribution.occurredAt, note = contribution.note),
                    )
                }
            }
            file.subscriptions.forEach { item ->
                val merchantId = item.merchantName?.let { name ->
                    val normalized = normalizeMerchant(name)
                    database.merchantDao().findByName(normalized)?.id
                }
                database.subscriptionDao().insert(
                    SubscriptionEntity(
                        merchantId = merchantId,
                        categoryId = item.categoryName?.let { categories[it]?.id },
                        displayName = item.displayName,
                        typicalAmount = item.typicalAmount,
                        cadence = item.cadence,
                        status = item.status,
                        lastSeenAt = item.lastSeenAt,
                        nextExpectedAt = item.nextExpectedAt,
                        createdAt = item.createdAt,
                    ),
                )
            }
        }
    }
}
