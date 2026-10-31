package co.luka.data.repository

import co.luka.capture.CaptureDraft
import co.luka.capture.IngestStore
import co.luka.capture.SourceSnapshot
import co.luka.data.alerts.BudgetWatcher
import co.luka.data.db.CaptureEntity
import co.luka.data.db.LukaDatabase
import co.luka.data.db.MerchantEntity
import co.luka.data.db.newMovement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomIngestStore @Inject constructor(
    private val database: LukaDatabase,
    private val budgets: BudgetWatcher,
) : IngestStore {
    override suspend fun sourceForPackage(packageName: String): SourceSnapshot? {
        val source = database.sourceDao().findByPackage(packageName) ?: return null
        return SourceSnapshot(source.id, source.parserId, source.monitorNotifications)
    }

    override suspend fun captureExists(dedupeKey: String): Boolean =
        database.captureDao().countKey(dedupeKey) > 0

    override suspend fun movementExists(dedupeKey: String): Boolean =
        database.movementDao().countActiveKey(dedupeKey) > 0

    override suspend fun categoryForMerchant(normalizedName: String): Long? =
        database.merchantDao().findByName(normalizedName)?.defaultCategoryId

    override suspend fun touchMerchant(normalizedName: String, displayName: String): Long {
        val now = System.currentTimeMillis()
        val existing = database.merchantDao().findByName(normalizedName)
        if (existing == null) {
            return database.merchantDao().insert(
                MerchantEntity(
                    normalizedName = normalizedName,
                    displayName = displayName,
                    defaultCategoryId = null,
                    learnedFrom = "auto",
                    confirmations = 1,
                    updatedAt = now,
                ),
            )
        }
        database.merchantDao().update(
            existing.copy(confirmations = existing.confirmations + 1, updatedAt = now),
        )
        return existing.id
    }

    override suspend fun categoryName(categoryId: Long): String? =
        database.categoryDao().find(categoryId)?.name

    override suspend fun saveCapture(
        draft: CaptureDraft,
        dedupeKey: String,
        status: String,
        parserId: String?,
        sourceId: Long?,
        movementId: Long?,
    ): Long = database.captureDao().insert(
        CaptureEntity(
            channel = draft.channel,
            sourceId = sourceId,
            packageName = draft.packageName,
            sender = draft.sender,
            title = draft.title,
            body = draft.body,
            postedAt = draft.postedAt,
            dedupeKey = dedupeKey,
            parseStatus = status,
            parserId = parserId,
            movementId = movementId,
            createdAt = System.currentTimeMillis(),
        ),
    )

    override suspend fun saveMovement(
        direction: String,
        amount: Long,
        occurredAt: Long,
        categoryId: Long?,
        merchantId: Long?,
        counterparty: String?,
        sourceId: Long?,
        incomeKind: String?,
        origin: String,
        captureId: Long?,
        dedupeKey: String,
    ): Long {
        val id = database.movementDao().insert(
            newMovement(
                direction = direction,
                amount = amount,
                occurredAt = occurredAt,
                categoryId = categoryId,
                merchantId = merchantId,
                counterparty = counterparty,
                note = null,
                sourceId = sourceId,
                incomeKind = incomeKind,
                origin = origin,
                status = "confirmed",
                captureId = captureId,
                dedupeKey = dedupeKey,
            ),
        )
        if (direction == "expense") budgets.onExpense(categoryId, occurredAt)
        return id
    }

    override suspend fun linkCapture(captureId: Long, movementId: Long) {
        database.captureDao().link(captureId, movementId, "parsed")
    }
}
