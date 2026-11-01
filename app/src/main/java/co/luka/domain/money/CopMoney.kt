package co.luka.domain.money

import co.luka.domain.text.indexOfPhrase
import co.luka.domain.text.stripAccents
import kotlin.math.abs

object CopMoney {
    private val withSymbol = Regex("""\$\s*(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{2}))?""")
    private val plain = Regex("""(?<![\d.])(\d{1,3}(?:\.\d{3})+|\d{4,})(?!\d)""")
    private val verbs = listOf(
        "pagaste",
        "compraste",
        "enviaste",
        "has enviado",
        "tu envio de",
        "envio exitoso",
        "envio realizado",
        "transferencia enviada",
        "recibiste",
        "te enviaron",
        "te transfirieron",
        "te depositaron",
        "depositaron",
        "consignaron",
        "abonaste",
        "abonaron",
        "transferiste",
        "retiraste",
        "te debitaron",
        "transferencia recibida",
        "te llegaron",
        "te llego",
        "consignacion",
        "compra",
        "debito",
        "abono",
        "pago",
    )

    fun parseFirst(text: String): Long? {
        val normalized = stripAccents(text).lowercase()
        val symbolMatches = withSymbol.findAll(normalized).toList()
        if (symbolMatches.isNotEmpty()) {
            return amountOf(pickNearVerb(normalized, symbolMatches))
        }
        val plainMatch = plain.find(normalized) ?: return null
        return amountOf(plainMatch)
    }

    // El primer $ suele ser el saldo. El monto del movimiento es el que queda junto al verbo.
    private fun pickNearVerb(text: String, matches: List<MatchResult>): MatchResult {
        if (matches.size == 1) return matches.first()
        val verbAt = verbs.mapNotNull { phrase -> indexOfPhrase(text, phrase).takeIf { it >= 0 } }.minOrNull()
            ?: return matches.first()
        val afterVerb = matches.filter { it.range.first >= verbAt }
        val pool = if (afterVerb.isNotEmpty()) afterVerb else matches
        return pool.minBy { abs(it.range.first - verbAt) }
    }

    private fun amountOf(match: MatchResult): Long? =
        match.groupValues[1].replace(".", "").toLongOrNull()?.takeIf { it > 0L }

    fun format(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val digits = abs(amount).toString()
        val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
        return sign + "$" + grouped
    }
}
