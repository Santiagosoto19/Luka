package co.luka.domain.dedupe

import co.luka.domain.merchant.normalizeMerchant
import co.luka.domain.model.Direction
import java.security.MessageDigest

object DedupeKeys {
    const val BLOCK_MS: Long = 10L * 60L * 1000L

    fun captureKey(channel: String, packageName: String, postedAt: Long, body: String): String =
        sha256("$channel|$packageName|$postedAt|$body")

    fun movementKey(direction: Direction, amount: Long, counterparty: String?, occurredAt: Long): String {
        val block = Math.floorDiv(occurredAt, BLOCK_MS)
        val party = normalizeMerchant(counterparty.orEmpty())
        return sha256("${direction.wire}|$amount|$party|$block")
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
