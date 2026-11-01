package co.luka.capture

import co.luka.capture.parser.ParserRegistry
import co.luka.domain.dedupe.DedupeKeys
import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.model.incomeKindFor
import javax.inject.Inject
import javax.inject.Singleton

data class CaptureDraft(
    val channel: String,
    val packageName: String,
    val sender: String?,
    val title: String,
    val body: String,
    val postedAt: Long,
)

data class SourceSnapshot(
    val id: Long,
    val parserId: String?,
    val monitorNotifications: Boolean,
)

enum class CaptureOutcome {
    PARSED,
    REVIEW,
    DUPLICATE,
    SKIPPED,
}

interface IngestStore {
    suspend fun sourceForPackage(packageName: String): SourceSnapshot?
    suspend fun captureExists(dedupeKey: String): Boolean
    suspend fun movementExists(dedupeKey: String): Boolean
    suspend fun categoryForMerchant(normalizedName: String): Long?
    suspend fun touchMerchant(normalizedName: String, displayName: String): Long
    suspend fun categoryName(categoryId: Long): String?
    suspend fun saveCapture(
        draft: CaptureDraft,
        dedupeKey: String,
        status: String,
        parserId: String?,
        sourceId: Long?,
        movementId: Long?,
    ): Long

    suspend fun saveMovement(
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
    ): Long

    suspend fun linkCapture(captureId: Long, movementId: Long)
}

@Singleton
class IngestEngine @Inject constructor(
    private val parsers: ParserRegistry,
) {
    suspend fun ingest(draft: CaptureDraft, store: IngestStore): CaptureOutcome {
        val source = store.sourceForPackage(draft.packageName) ?: return CaptureOutcome.SKIPPED
        if (draft.channel == "notification" && !source.monitorNotifications) return CaptureOutcome.SKIPPED
        val captureKey = DedupeKeys.captureKey(draft.channel, draft.packageName, draft.postedAt, draft.body)
        if (store.captureExists(captureKey)) return CaptureOutcome.SKIPPED

        val parser = parsers.byId(source.parserId)
        val parsed = parser?.parse(draft.title, draft.body, draft.postedAt)
        if (parsed == null) {
            store.saveCapture(draft, captureKey, "ignored", parser?.id ?: source.parserId, source.id, null)
            return CaptureOutcome.SKIPPED
        }

        val occurredAt = parsed.occurredAt ?: draft.postedAt
        val movementKey = DedupeKeys.movementKey(parsed.direction, parsed.amount, parsed.counterparty, occurredAt)
        if (store.movementExists(movementKey)) {
            store.saveCapture(draft, captureKey, "duplicate", parser.id, source.id, null)
            return CaptureOutcome.DUPLICATE
        }

        val display = parsed.counterparty?.trim()?.takeIf { it.isNotEmpty() }
        val normalized = display?.let { normalizeMerchant(it) }
        val categoryId = normalized?.let { store.categoryForMerchant(it) }
        val merchantId = if (normalized != null && display != null) store.touchMerchant(normalized, display) else null
        val incomeKind = incomeKindFor(parsed.direction, categoryId?.let { store.categoryName(it) })
        val captureId = store.saveCapture(draft, captureKey, "parsed", parser.id, source.id, null)
        val movementId = store.saveMovement(
            direction = parsed.direction.wire,
            amount = parsed.amount,
            occurredAt = occurredAt,
            categoryId = categoryId,
            merchantId = merchantId,
            counterparty = display,
            sourceId = source.id,
            incomeKind = incomeKind,
            origin = if (draft.channel == "sms") "sms" else "notification",
            captureId = captureId,
            dedupeKey = movementKey,
        )
        store.linkCapture(captureId, movementId)
        return CaptureOutcome.PARSED
    }
}
