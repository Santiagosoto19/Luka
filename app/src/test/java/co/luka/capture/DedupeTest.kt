package co.luka.capture

import co.luka.capture.parser.NequiParser
import co.luka.capture.parser.ParserRegistry
import co.luka.domain.dedupe.DedupeKeys
import co.luka.domain.model.Direction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DedupeTest {
    @Test
    fun movementKeyCollapsesTenMinuteBlockAndMerchantSpelling() {
        val block = DedupeKeys.BLOCK_MS
        val aligned = 1_700_000_000_000L / block * block
        val first = DedupeKeys.movementKey(Direction.EXPENSE, 25_000, "Tienda X", aligned)
        val inside = DedupeKeys.movementKey(Direction.EXPENSE, 25_000, "tienda  x", aligned + 9 * 60 * 1000)
        val nextBlock = DedupeKeys.movementKey(Direction.EXPENSE, 25_000, "Tienda X", aligned + block)
        assertEquals(first, inside)
        assertNotEquals(first, nextBlock)
    }

    @Test
    fun samePushTwiceDoesNotDuplicate() = runBlocking {
        val store = MemoryIngestStore()
        val engine = IngestEngine(ParserRegistry())
        val draft = nequi("Pagaste $25.000 a Tienda X con tu Nequi", 1_700_000_000_000)
        engine.ingest(draft, store)
        engine.ingest(draft, store)
        assertEquals(1, store.movements.size)
        assertEquals(1, store.captures.size)
    }

    @Test
    fun sameAmountInsideTenMinutesDoesNotCreateAnotherMovement() = runBlocking {
        val store = MemoryIngestStore()
        val engine = IngestEngine(ParserRegistry())
        val block = DedupeKeys.BLOCK_MS
        val aligned = 1_700_000_000_000L / block * block
        engine.ingest(nequi("Pagaste $25.000 a Tienda X con tu Nequi", aligned), store)
        val second = engine.ingest(nequi("Pagaste $25.000 a Tienda X con tu Nequi hoy", aligned + 60_000), store)
        assertEquals(CaptureOutcome.DUPLICATE, second)
        assertEquals(1, store.movements.size)
        assertEquals(2, store.captures.size)
        assertEquals("duplicate", store.captures[1].status)
    }

    @Test
    fun unrecognizedTextStaysInReview() = runBlocking {
        val store = MemoryIngestStore()
        val engine = IngestEngine(ParserRegistry())
        val outcome = engine.ingest(
            CaptureDraft("notification", "com.bancolombia.bancolombiaapp", null, "", "Hoy es un buen día para ahorrar", 10),
            store,
        )
        assertEquals(CaptureOutcome.REVIEW, outcome)
        assertEquals(0, store.movements.size)
        assertEquals("review", store.captures.single().status)
        assertEquals("Hoy es un buen día para ahorrar", store.captures.single().body)
    }

    private fun nequi(body: String, postedAt: Long) = CaptureDraft(
        channel = "notification",
        packageName = NequiParser.PACKAGE_NAME,
        sender = null,
        title = "",
        body = body,
        postedAt = postedAt,
    )
}

private class MemoryIngestStore : IngestStore {
    val captures = mutableListOf<SavedCapture>()
    val movements = mutableListOf<String>()
    private val merchants = mutableMapOf<String, Long?>()

    override suspend fun sourceForPackage(packageName: String): SourceSnapshot? =
        if (packageName == NequiParser.PACKAGE_NAME || packageName == "com.bancolombia.bancolombiaapp") {
            SourceSnapshot(id = 1, parserId = if (packageName == NequiParser.PACKAGE_NAME) "nequi" else "bancolombia", monitorNotifications = true)
        } else {
            null
        }

    override suspend fun captureExists(dedupeKey: String): Boolean = captures.any { it.key == dedupeKey }

    override suspend fun movementExists(dedupeKey: String): Boolean = movements.contains(dedupeKey)

    override suspend fun categoryForMerchant(normalizedName: String): Long? = merchants[normalizedName]

    override suspend fun touchMerchant(normalizedName: String, displayName: String): Long {
        if (!merchants.containsKey(normalizedName)) merchants[normalizedName] = null
        return 1
    }

    override suspend fun categoryName(categoryId: Long): String? = null

    override suspend fun saveCapture(
        draft: CaptureDraft,
        dedupeKey: String,
        status: String,
        parserId: String?,
        sourceId: Long?,
        movementId: Long?,
    ): Long {
        captures += SavedCapture(dedupeKey, status, draft.body)
        return captures.size.toLong()
    }

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
        movements += dedupeKey
        return movements.size.toLong()
    }

    override suspend fun linkCapture(captureId: Long, movementId: Long) = Unit
}

private data class SavedCapture(val key: String, val status: String, val body: String)
