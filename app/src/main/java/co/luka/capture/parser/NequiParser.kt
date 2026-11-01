package co.luka.capture.parser

import co.luka.domain.model.Direction

object NequiParser : MovementParser {
    const val PACKAGE_NAME = "com.nequi.MobileApp"
    override val id: String = "nequi"

    private val payeeBeforeNequi = Regex("""(?i)\ba\s+(.+?)\s+con tu\b""")
    private val payeeToEnd = Regex("""(?is)\ba\s+(.+?)\s*$""")
    private val payer = Regex("""(?is)\bde\s+(.+?)\s*$""")

    override fun match(packageName: String?, title: String, body: String): Boolean =
        packageName == PACKAGE_NAME

    override fun parse(title: String, body: String, postedAt: Long): ParsedMovement? {
        val text = listOf(title, body).filter { it.isNotBlank() }.joinToString(" ")
        val incoming = DirectionWords.detect(text) == Direction.INCOME
        return parseMovement(title, body) { raw ->
            if (incoming) {
                firstGroup(payer, raw) ?: firstGroup(payeeToEnd, raw)
            } else {
                firstGroup(payeeBeforeNequi, raw) ?: firstGroup(payeeToEnd, raw)
            }
        }
    }
}
