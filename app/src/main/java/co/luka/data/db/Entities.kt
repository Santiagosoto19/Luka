package co.luka.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sources",
    indices = [Index(value = ["slug"], unique = true)],
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slug: String,
    val name: String,
    val kind: String,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "parser_id") val parserId: String?,
    val color: String,
    val icon: String,
    @ColumnInfo(name = "monitor_notifications") val monitorNotifications: Boolean,
    @ColumnInfo(name = "sms_sender") val smsSender: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val archived: Boolean,
    @ColumnInfo(name = "opening_balance", defaultValue = "0") val openingBalance: Long = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val direction: String,
    val icon: String,
    val color: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_system") val isSystem: Boolean,
    val archived: Boolean,
)

@Entity(
    tableName = "merchants",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "default_category_id") val defaultCategoryId: Long?,
    @ColumnInfo(name = "learned_from") val learnedFrom: String,
    val confirmations: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "captures",
    indices = [Index(value = ["dedupe_key"], unique = true)],
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: String,
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    @ColumnInfo(name = "package_name") val packageName: String?,
    val sender: String?,
    val title: String,
    val body: String,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String,
    @ColumnInfo(name = "parse_status") val parseStatus: String,
    @ColumnInfo(name = "parser_id") val parserId: String?,
    @ColumnInfo(name = "movement_id") val movementId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,
    val amount: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "occurred_day") val occurredDay: String,
    @ColumnInfo(name = "occurred_month") val occurredMonth: String,
    val weekday: Int,
    val hour: Int,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "merchant_id") val merchantId: Long?,
    val counterparty: String?,
    val note: String?,
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    @ColumnInfo(name = "income_kind") val incomeKind: String?,
    val origin: String,
    val status: String,
    @ColumnInfo(name = "capture_id") val captureId: Long?,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "counter_source_id") val counterSourceId: Long? = null,
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category_id", "period"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "limit_amount") val limitAmount: Long,
    val period: String,
    @ColumnInfo(name = "alert_ratio") val alertRatio: Double,
    val active: Boolean,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_amount") val targetAmount: Long,
    @ColumnInfo(name = "deadline_day") val deadlineDay: String?,
    val icon: String,
    val color: String,
    val archived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "goal_contributions")
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    val amount: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val note: String?,
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "merchant_id") val merchantId: Long?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "typical_amount") val typicalAmount: Long,
    val cadence: String,
    val status: String,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long?,
    @ColumnInfo(name = "next_expected_at") val nextExpectedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class MovementListRow(
    val id: Long,
    val direction: String,
    val amount: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "occurred_day") val occurredDay: String,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_color") val categoryColor: String?,
    val counterparty: String?,
    val note: String?,
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "income_kind") val incomeKind: String?,
    val status: String,
    val origin: String,
    @ColumnInfo(name = "counter_source_name") val counterSourceName: String?,
)

data class DirectionSum(val direction: String, val total: Long)

data class SourceDirectionSum(
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    val direction: String,
    val total: Long,
)

data class SourceBalanceParts(
    @ColumnInfo(name = "source_id") val sourceId: Long,
    val income: Long,
    val expense: Long,
    @ColumnInfo(name = "adjustment_in") val adjustmentIn: Long,
    @ColumnInfo(name = "adjustment_out") val adjustmentOut: Long,
    @ColumnInfo(name = "transfer_out") val transferOut: Long,
    @ColumnInfo(name = "transfer_in") val transferIn: Long,
)
data class CategoryTotal(
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val name: String?,
    val color: String?,
    val total: Long,
)
data class MonthSum(val month: String, val direction: String, val total: Long)
data class DaySum(val day: String, val direction: String, val total: Long)
data class MerchantSum(
    @ColumnInfo(name = "merchant_id") val merchantId: Long,
    val name: String,
    val total: Long,
)
data class SourceTotal(
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    val name: String?,
    val total: Long,
)
data class HourSum(val hour: Int, val total: Long)
data class WeekdaySum(val weekday: Int, val total: Long)
data class IncomeKindSum(
    @ColumnInfo(name = "income_kind") val incomeKind: String?,
    val total: Long,
)
data class TopExpense(
    val amount: Long,
    val counterparty: String?,
    @ColumnInfo(name = "occurred_day") val occurredDay: String,
)
data class DayExpense(val day: String, val expense: Long)
data class BudgetProgress(
    val id: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    val color: String,
    @ColumnInfo(name = "limit_amount") val limitAmount: Long,
    @ColumnInfo(name = "alert_ratio") val alertRatio: Double,
    val spent: Long,
)
data class GoalProgress(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "target_amount") val targetAmount: Long,
    val color: String,
    val icon: String,
    @ColumnInfo(name = "deadline_day") val deadlineDay: String?,
    val saved: Long,
)
data class MerchantSpend(
    @ColumnInfo(name = "merchant_id") val merchantId: Long,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val amount: Long,
)
