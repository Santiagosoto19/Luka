package co.luka.capture.parser

import co.luka.domain.model.Direction
import co.luka.domain.money.CopMoney
import co.luka.domain.text.indexOfPhrase
import co.luka.domain.text.stripAccents

data class ParsedMovement(
    val direction: Direction,
    val amount: Long,
    val counterparty: String?,
    val occurredAt: Long? = null,
)

interface MovementParser {
    val id: String
    fun match(packageName: String?, title: String, body: String): Boolean
    fun parse(title: String, body: String, postedAt: Long): ParsedMovement?
}

internal object DirectionWords {
    private val rules = listOf(
        "te llegaron" to Direction.INCOME,
        "te llego" to Direction.INCOME,
        "transferencia recibida" to Direction.INCOME,
        "consignacion" to Direction.INCOME,
        "recibiste" to Direction.INCOME,
        "te enviaron" to Direction.INCOME,
        "te transfirieron" to Direction.INCOME,
        "te depositaron" to Direction.INCOME,
        "depositaron" to Direction.INCOME,
        "consignaron" to Direction.INCOME,
        "abonaron" to Direction.INCOME,
        "abono" to Direction.INCOME,
        "abonaste" to Direction.EXPENSE,
        "pagaste" to Direction.EXPENSE,
        "compraste" to Direction.EXPENSE,
        "enviaste" to Direction.EXPENSE,
        "has enviado" to Direction.EXPENSE,
        "tu envio de" to Direction.EXPENSE,
        "envio exitoso" to Direction.EXPENSE,
        "envio realizado" to Direction.EXPENSE,
        "transferencia enviada" to Direction.EXPENSE,
        "transferiste" to Direction.EXPENSE,
        "retiraste" to Direction.EXPENSE,
        "te debitaron" to Direction.EXPENSE,
        "compra" to Direction.EXPENSE,
        "debito" to Direction.EXPENSE,
        "pago" to Direction.EXPENSE,
    )

    fun detect(text: String): Direction? {
        val normalized = stripAccents(text.lowercase())
        // Cupo, rechazos y recordatorios no son movimientos. indexOf suelto los confirmaba.
        if (isNotice(normalized)) return null
        val direction = rules.mapNotNull { (phrase, direction) ->
            val index = indexOfPhrase(normalized, phrase)
            if (index >= 0) index to direction else null
        }.minByOrNull { it.first }?.second ?: return null
        if (direction == Direction.INCOME && normalized.contains("a tu credito")) return null
        return direction
    }

    private fun isNotice(normalized: String): Boolean =
        normalized.contains("rechazad") ||
            indexOfPhrase(normalized, "cupo") >= 0 ||
            indexOfPhrase(normalized, "recuerda") >= 0
}

internal fun parseMovement(title: String, body: String, counterparty: (String) -> String?): ParsedMovement? {
    val text = listOf(title, body).filter { it.isNotBlank() }.joinToString(" ")
    val amount = CopMoney.parseFirst(text) ?: return null
    val direction = DirectionWords.detect(text) ?: return null
    val party = counterparty(text)?.let(::clipCounterparty)
    return ParsedMovement(direction, amount, party, null)
}

private val counterpartyEnd = Regex("""(?i)[.\r\n]|t[uú] saldo|\bfue\b""")

internal fun clipCounterparty(raw: String): String? {
    val end = counterpartyEnd.find(raw)?.range?.first ?: raw.length
    return raw.substring(0, end).trim().trimEnd('.', ',', ';').takeIf { it.isNotEmpty() }
}

internal fun firstGroup(pattern: Regex, text: String): String? = pattern.find(text)?.groupValues?.getOrNull(1)
