package co.luka.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int

    @Query("SELECT * FROM sources ORDER BY sort_order, name")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY sort_order, name")
    suspend fun all(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE package_name = :packageName AND archived = 0 LIMIT 1")
    suspend fun findByPackage(packageName: String): SourceEntity?

    @Query("SELECT * FROM sources WHERE slug = :slug LIMIT 1")
    suspend fun findBySlug(slug: String): SourceEntity?

    @Query("SELECT * FROM sources WHERE kind = 'cash' ORDER BY sort_order LIMIT 1")
    suspend fun cash(): SourceEntity?

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun find(id: Long): SourceEntity?

    @Insert
    suspend fun insertAll(items: List<SourceEntity>)

    @Update
    suspend fun update(item: SourceEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sort_order, name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun find(id: Long): CategoryEntity?

    @Insert
    suspend fun insertAll(items: List<CategoryEntity>)

    @Insert
    suspend fun insert(item: CategoryEntity): Long

    @Update
    suspend fun update(item: CategoryEntity)
}

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants WHERE normalized_name = :name LIMIT 1")
    suspend fun findByName(name: String): MerchantEntity?

    @Insert
    suspend fun insert(item: MerchantEntity): Long

    @Update
    suspend fun update(item: MerchantEntity)

    @Query("SELECT * FROM merchants")
    suspend fun all(): List<MerchantEntity>
}

@Dao
interface CaptureDao {
    @Query("SELECT COUNT(*) FROM captures WHERE dedupe_key = :key")
    suspend fun countKey(key: String): Int

    @Insert
    suspend fun insert(item: CaptureEntity): Long

    @Query("UPDATE captures SET movement_id = :movementId, parse_status = :status WHERE id = :id")
    suspend fun link(id: Long, movementId: Long?, status: String)

    @Query("SELECT * FROM captures WHERE parse_status = 'review' ORDER BY posted_at DESC")
    fun observeReview(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun find(id: Long): CaptureEntity?
}

@Dao
interface MovementDao {
    @Query("SELECT COALESCE(MAX(updated_at), 0) FROM movements")
    fun revision(): Flow<Long>

    @Query("SELECT COUNT(*) FROM movements WHERE dedupe_key = :key AND deleted_at IS NULL")
    suspend fun countActiveKey(key: String): Int

    @Insert
    suspend fun insert(item: MovementEntity): Long

    @Update
    suspend fun update(item: MovementEntity)

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun find(id: Long): MovementEntity?

    @Query("UPDATE movements SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("UPDATE movements SET deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)

    @Query(
        """
        SELECT m.id, m.direction, m.amount, m.occurred_at, m.occurred_day,
               m.category_id, c.name AS category_name, c.color AS category_color,
               m.counterparty, m.note, m.source_id, s.name AS source_name,
               m.income_kind, m.status, m.origin, cs.name AS counter_source_name
        FROM movements m
        LEFT JOIN categories c ON c.id = m.category_id
        LEFT JOIN sources s ON s.id = m.source_id
        LEFT JOIN sources cs ON cs.id = m.counter_source_id
        WHERE m.deleted_at IS NULL AND m.status = 'confirmed'
        ORDER BY m.occurred_at DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<MovementListRow>>

    @Query(
        """
        SELECT m.id, m.direction, m.amount, m.occurred_at, m.occurred_day,
               m.category_id, c.name AS category_name, c.color AS category_color,
               m.counterparty, m.note, m.source_id, s.name AS source_name,
               m.income_kind, m.status, m.origin, cs.name AS counter_source_name
        FROM movements m
        LEFT JOIN categories c ON c.id = m.category_id
        LEFT JOIN sources s ON s.id = m.source_id
        LEFT JOIN sources cs ON cs.id = m.counter_source_id
        WHERE m.deleted_at IS NULL
          AND (:fromDay IS NULL OR m.occurred_day >= :fromDay)
          AND (:toDay IS NULL OR m.occurred_day <= :toDay)
          AND (:categoryId IS NULL OR m.category_id = :categoryId)
          AND (:sourceId IS NULL OR m.source_id = :sourceId)
          AND (:direction IS NULL OR m.direction = :direction)
          AND (:minAmount IS NULL OR m.amount >= :minAmount)
          AND (:maxAmount IS NULL OR m.amount <= :maxAmount)
        ORDER BY m.occurred_at DESC
        """,
    )
    fun observeFiltered(
        fromDay: String?,
        toDay: String?,
        categoryId: Long?,
        sourceId: Long?,
        direction: String?,
        minAmount: Long?,
        maxAmount: Long?,
    ): Flow<List<MovementListRow>>

    @Query(
        """
        SELECT direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY direction
        """,
    )
    suspend fun sumsByDirection(start: String, endExclusive: String): List<DirectionSum>

    @Query(
        """
        SELECT direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_day >= :start AND occurred_day < :endExclusive
          AND (:sourceId IS NULL OR source_id = :sourceId)
        GROUP BY direction
        """,
    )
    suspend fun sumsByDirectionFiltered(start: String, endExclusive: String, sourceId: Long?): List<DirectionSum>

    @Query(
        """
        SELECT source_id, direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY source_id, direction
        """,
    )
    suspend fun sumsBySourceAndDirection(start: String, endExclusive: String): List<SourceDirectionSum>

    @Query(
        """
        SELECT occurred_day AS day, direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_day >= :start AND occurred_day < :endExclusive
          AND (:sourceId IS NULL OR source_id = :sourceId)
        GROUP BY occurred_day, direction
        ORDER BY occurred_day
        """,
    )
    suspend fun sumsByDayFiltered(start: String, endExclusive: String, sourceId: Long?): List<DaySum>

    @Query(
        """
        SELECT m.id, m.direction, m.amount, m.occurred_at, m.occurred_day,
               m.category_id, c.name AS category_name, c.color AS category_color,
               m.counterparty, m.note, m.source_id, s.name AS source_name,
               m.income_kind, m.status, m.origin, cs.name AS counter_source_name
        FROM movements m
        LEFT JOIN categories c ON c.id = m.category_id
        LEFT JOIN sources s ON s.id = m.source_id
        LEFT JOIN sources cs ON cs.id = m.counter_source_id
        WHERE m.deleted_at IS NULL AND m.status = 'confirmed'
          AND m.occurred_day >= :start AND m.occurred_day < :endExclusive
          AND (:sourceId IS NULL OR m.source_id = :sourceId)
        ORDER BY m.occurred_at DESC
        LIMIT :limit
        """,
    )
    suspend fun recentInRange(start: String, endExclusive: String, sourceId: Long?, limit: Int): List<MovementListRow>

    @Query(
        """
        SELECT m.category_id, c.name AS name, c.color AS color, COALESCE(SUM(m.amount), 0) AS total
        FROM movements m
        LEFT JOIN categories c ON c.id = m.category_id
        WHERE m.status = 'confirmed' AND m.deleted_at IS NULL AND m.direction = :direction AND m.origin != 'adjustment'
          AND m.occurred_day >= :start AND m.occurred_day < :endExclusive
        GROUP BY m.category_id
        ORDER BY total DESC
        """,
    )
    suspend fun sumsByCategory(direction: String, start: String, endExclusive: String): List<CategoryTotal>

    @Query(
        """
        SELECT occurred_month AS month, direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_month >= :fromMonth
        GROUP BY occurred_month, direction
        ORDER BY occurred_month
        """,
    )
    suspend fun sumsByMonth(fromMonth: String): List<MonthSum>

    @Query(
        """
        SELECT occurred_day AS day, direction, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL
          AND origin != 'adjustment'
          AND direction IN ('income', 'expense')
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY occurred_day, direction
        ORDER BY occurred_day
        """,
    )
    suspend fun sumsByDay(start: String, endExclusive: String): List<DaySum>

    @Query(
        """
        SELECT occurred_day AS day, COALESCE(SUM(amount), 0) AS expense
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment'
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY occurred_day
        ORDER BY occurred_day
        """,
    )
    suspend fun expenseByDay(start: String, endExclusive: String): List<DayExpense>

    @Query(
        """
        SELECT m.merchant_id, COALESCE(mer.display_name, m.counterparty, 'Sin comercio') AS name,
               COALESCE(SUM(m.amount), 0) AS total
        FROM movements m
        LEFT JOIN merchants mer ON mer.id = m.merchant_id
        WHERE m.status = 'confirmed' AND m.deleted_at IS NULL AND m.direction = 'expense' AND m.origin != 'adjustment'
          AND m.merchant_id IS NOT NULL
          AND m.occurred_day >= :start AND m.occurred_day < :endExclusive
        GROUP BY m.merchant_id
        ORDER BY total DESC
        LIMIT 8
        """,
    )
    suspend fun topMerchants(start: String, endExclusive: String): List<MerchantSum>

    @Query(
        """
        SELECT m.source_id, COALESCE(s.name, 'Sin entidad') AS name, COALESCE(SUM(m.amount), 0) AS total
        FROM movements m
        LEFT JOIN sources s ON s.id = m.source_id
        WHERE m.status = 'confirmed' AND m.deleted_at IS NULL AND m.direction = 'expense' AND m.origin != 'adjustment'
          AND m.occurred_day >= :start AND m.occurred_day < :endExclusive
        GROUP BY m.source_id
        ORDER BY total DESC
        """,
    )
    suspend fun sumsBySource(start: String, endExclusive: String): List<SourceTotal>

    @Query(
        """
        SELECT hour, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment'
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY hour
        """,
    )
    suspend fun sumsByHour(start: String, endExclusive: String): List<HourSum>

    @Query(
        """
        SELECT weekday, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment'
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY weekday
        """,
    )
    suspend fun sumsByWeekday(start: String, endExclusive: String): List<WeekdaySum>

    @Query(
        """
        SELECT income_kind, COALESCE(SUM(amount), 0) AS total
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'income' AND origin != 'adjustment'
          AND occurred_day >= :start AND occurred_day < :endExclusive
        GROUP BY income_kind
        """,
    )
    suspend fun sumsByIncomeKind(start: String, endExclusive: String): List<IncomeKindSum>

    @Query(
        """
        SELECT amount, counterparty, occurred_day
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment'
          AND occurred_day >= :start AND occurred_day < :endExclusive
        ORDER BY amount DESC
        LIMIT 1
        """,
    )
    suspend fun topExpense(start: String, endExclusive: String): TopExpense?

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM movements
        WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment'
          AND category_id = :categoryId AND occurred_month = :month
        """,
    )
    suspend fun sumCategoryMonth(categoryId: Long, month: String): Long

    @Query(
        """
        SELECT m.merchant_id, mer.normalized_name, mer.display_name, m.occurred_at, m.amount
        FROM movements m
        JOIN merchants mer ON mer.id = m.merchant_id
        WHERE m.status = 'confirmed' AND m.deleted_at IS NULL AND m.direction = 'expense' AND m.origin != 'adjustment'
          AND m.merchant_id IN (
            SELECT merchant_id FROM movements
            WHERE status = 'confirmed' AND deleted_at IS NULL AND direction = 'expense' AND origin != 'adjustment' AND merchant_id IS NOT NULL
            GROUP BY merchant_id HAVING COUNT(*) >= 3
          )
        ORDER BY m.merchant_id, m.occurred_at
        """,
    )
    suspend fun recurringCandidates(): List<MerchantSpend>

    @Query(
        """
        SELECT m.id, m.direction, m.amount, m.occurred_at, m.occurred_day, m.category_id,
               c.name AS category_name, c.color AS category_color, m.counterparty, m.note,
               m.source_id, s.name AS source_name, m.income_kind, m.status, m.origin,
               cs.name AS counter_source_name
        FROM movements m
        LEFT JOIN categories c ON c.id = m.category_id
        LEFT JOIN sources s ON s.id = m.source_id
        LEFT JOIN sources cs ON cs.id = m.counter_source_id
        WHERE m.deleted_at IS NULL
          AND m.occurred_day >= :start AND m.occurred_day < :endExclusive
        ORDER BY m.occurred_at
        """,
    )
    suspend fun exportRows(start: String, endExclusive: String): List<MovementListRow>

    @Query(
        """
        SELECT s.id AS source_id,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'income' AND m.origin != 'adjustment'), 0) AS income,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'expense' AND m.origin != 'adjustment'), 0) AS expense,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'income' AND m.origin = 'adjustment'), 0) AS adjustment_in,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'expense' AND m.origin = 'adjustment'), 0) AS adjustment_out,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'transfer'), 0) AS transfer_out,
          COALESCE((SELECT SUM(m.amount) FROM movements m
            WHERE m.counter_source_id = s.id AND m.status = 'confirmed' AND m.deleted_at IS NULL
              AND m.direction = 'transfer'), 0) AS transfer_in
        FROM sources s
        """,
    )
    suspend fun balanceParts(): List<SourceBalanceParts>

    @Query("SELECT * FROM movements WHERE deleted_at IS NULL ORDER BY occurred_at")
    suspend fun allActive(): List<MovementEntity>
}

@Dao
interface BudgetDao {
    @Query("SELECT COALESCE(MAX(id), 0) FROM budgets")
    fun revision(): Flow<Long>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period = :period LIMIT 1")
    suspend fun find(categoryId: Long, period: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period = :period AND active = 1 LIMIT 1")
    suspend fun findActive(categoryId: Long, period: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE active = 1")
    suspend fun active(): List<BudgetEntity>

    @Insert
    suspend fun insert(item: BudgetEntity): Long

    @Update
    suspend fun update(item: BudgetEntity)

    @Query(
        """
        SELECT b.id, b.category_id, c.name AS category_name, c.color AS color,
               b.limit_amount, b.alert_ratio,
               (
                 SELECT COALESCE(SUM(m.amount), 0) FROM movements m
                 WHERE m.category_id = b.category_id AND m.direction = 'expense' AND m.origin != 'adjustment'
                   AND m.status = 'confirmed' AND m.deleted_at IS NULL
                   AND m.occurred_month = :month
               ) AS spent
        FROM budgets b
        JOIN categories c ON c.id = b.category_id
        WHERE b.period = 'month' AND b.active = 1
        ORDER BY c.sort_order
        """,
    )
    suspend fun progress(month: String): List<BudgetProgress>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>
}

@Dao
interface GoalDao {
    @Query("SELECT COALESCE(MAX(id), 0) FROM goals")
    fun revision(): Flow<Long>

    @Insert
    suspend fun insert(item: GoalEntity): Long

    @Update
    suspend fun update(item: GoalEntity)

    @Query("DELETE FROM goal_contributions WHERE goal_id = :goalId")
    suspend fun deleteContributions(goalId: Long)

    @Insert
    suspend fun insertContribution(item: GoalContributionEntity): Long

    @Query(
        """
        SELECT g.id, g.name, g.target_amount, g.color, g.icon, g.deadline_day,
               (SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goal_id = g.id) AS saved
        FROM goals g
        WHERE g.archived = 0
        ORDER BY g.created_at DESC
        """,
    )
    fun observeProgress(): Flow<List<GoalProgress>>

    @Query(
        """
        SELECT g.id, g.name, g.target_amount, g.color, g.icon, g.deadline_day,
               (SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goal_id = g.id) AS saved
        FROM goals g
        WHERE g.archived = 0
        """,
    )
    suspend fun progress(): List<GoalProgress>

    @Query("SELECT * FROM goals")
    suspend fun all(): List<GoalEntity>

    @Query("SELECT * FROM goal_contributions ORDER BY occurred_at")
    suspend fun contributions(): List<GoalContributionEntity>
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY status, display_name")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions")
    suspend fun all(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE merchant_id = :merchantId LIMIT 1")
    suspend fun findByMerchant(merchantId: Long): SubscriptionEntity?

    @Insert
    suspend fun insert(item: SubscriptionEntity): Long

    @Update
    suspend fun update(item: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions WHERE status = 'active' OR status = 'suggested'")
    suspend fun relevant(): List<SubscriptionEntity>
}
