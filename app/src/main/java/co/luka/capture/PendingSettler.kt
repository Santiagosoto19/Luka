package co.luka.capture

import co.luka.capture.parser.ParserRegistry
import co.luka.data.db.CaptureEntity
import co.luka.data.db.LukaDatabase
import co.luka.data.repository.RoomIngestStore
import co.luka.domain.dedupe.DedupeKeys
import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.model.incomeKindFor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingSettler @Inject constructor(
    private val database: LukaDatabase,
    private val parsers: ParserRegistry,
    private val store: RoomIngestStore,
) {
    suspend fun run() {
        database.captureDao().listReview().forEach { capture -> settle(capture) }
    }

    private suspend fun settle(capture: CaptureEntity) {
        val parser = parsers.byId(capture.parserId) ?: parsers.forPackage(capture.packageName)
        val parsed = parser?.parse(capture.title, capture.body, capture.postedAt)
        val sourceId = capture.sourceId
        if (parsed == null || parser == null || sourceId == null) {
            database.captureDao().link(capture.id, null, "ignored")
            return
        }
        val occurredAt = parsed.occurredAt ?: capture.postedAt
        val movementKey = DedupeKeys.movementKey(parsed.direction, parsed.amount, parsed.counterparty, occurredAt)
        if (store.movementExists(movementKey)) {
            database.captureDao().link(capture.id, null, "duplicate")
            return
        }
        val display = parsed.counterparty?.trim()?.takeIf { it.isNotEmpty() }
        val normalized = display?.let { normalizeMerchant(it) }
        val categoryId = normalized?.let { store.categoryForMerchant(it) }
        val merchantId = if (normalized != null && display != null) store.touchMerchant(normalized, display) else null
        val movementId = store.saveMovement(
            direction = parsed.direction.wire,
            amount = parsed.amount,
            occurredAt = occurredAt,
            categoryId = categoryId,
            merchantId = merchantId,
            counterparty = display,
            sourceId = sourceId,
            incomeKind = incomeKindFor(parsed.direction, categoryId?.let { store.categoryName(it) }),
            origin = if (capture.channel == "sms") "sms" else "notification",
            captureId = capture.id,
            dedupeKey = movementKey,
        )
        store.linkCapture(capture.id, movementId)
    }
}
