package co.luka.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SourceEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        CaptureEntity::class,
        MovementEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        SubscriptionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class LukaDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun captureDao(): CaptureDao
    abstract fun movementDao(): MovementDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun subscriptionDao(): SubscriptionDao
}

object LukaMigrations {
    // Room no corre migraciones al crear la base, y @Index no admite WHERE.
    // onCreate solo crea índices: la tabla nueva ya nace con las columnas.
    val partialIndexes = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            COLUMN_UPGRADES.forEach { db.execSQL(it) }
            INDEXES.forEach { db.execSQL(it) }
        }
    }

    fun createIndexes(db: SupportSQLiteDatabase) {
        INDEXES.forEach { db.execSQL(it) }
    }

    private val COLUMN_UPGRADES = listOf(
        "ALTER TABLE sources ADD COLUMN opening_balance INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE movements ADD COLUMN counter_source_id INTEGER",
    )

    private val INDEXES = listOf(
        """
        CREATE UNIQUE INDEX IF NOT EXISTS index_movements_dedupe_active
        ON movements(dedupe_key)
        WHERE dedupe_key IS NOT NULL AND deleted_at IS NULL
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_movements_confirmed_slice
        ON movements(occurred_at, direction, category_id, source_id, merchant_id, amount)
        WHERE status = 'confirmed' AND deleted_at IS NULL
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_movements_day_direction
        ON movements(occurred_day, direction)
        WHERE status = 'confirmed' AND deleted_at IS NULL
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_movements_month_direction
        ON movements(occurred_month, direction)
        WHERE status = 'confirmed' AND deleted_at IS NULL
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_movements_merchant_time
        ON movements(merchant_id, occurred_at)
        WHERE status = 'confirmed' AND deleted_at IS NULL AND merchant_id IS NOT NULL
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_captures_status_posted
        ON captures(parse_status, posted_at)
        """.trimIndent(),
    )
}
