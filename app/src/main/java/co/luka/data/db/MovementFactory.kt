package co.luka.data.db

import co.luka.domain.time.bucketsOf

fun newMovement(
    direction: String,
    amount: Long,
    occurredAt: Long,
    categoryId: Long?,
    merchantId: Long?,
    counterparty: String?,
    note: String?,
    sourceId: Long?,
    incomeKind: String?,
    origin: String,
    status: String,
    captureId: Long?,
    dedupeKey: String?,
    now: Long = System.currentTimeMillis(),
    counterSourceId: Long? = null,
): MovementEntity {
    val buckets = bucketsOf(occurredAt)
    return MovementEntity(
        direction = direction,
        amount = amount,
        occurredAt = buckets.occurredAt,
        occurredDay = buckets.occurredDay,
        occurredMonth = buckets.occurredMonth,
        weekday = buckets.weekday,
        hour = buckets.hour,
        categoryId = categoryId,
        merchantId = merchantId,
        counterparty = counterparty,
        note = note,
        sourceId = sourceId,
        incomeKind = incomeKind,
        origin = origin,
        status = status,
        captureId = captureId,
        dedupeKey = dedupeKey,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        counterSourceId = counterSourceId,
    )
}
