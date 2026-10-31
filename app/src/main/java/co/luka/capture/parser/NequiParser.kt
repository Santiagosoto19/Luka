package co.luka.capture.parser

object NequiParser : MovementParser {
    const val PACKAGE_NAME = "com.nequi.MobileApp"
    override val id: String = "nequi"

    private val payeeBeforeNequi = Regex("""(?i)\ba\s+(.+?)\s+con tu\b""")
    private val payeeToEnd = Regex("""(?is)\ba\s+(.+?)\s*$""")
    private val payer = Regex("""(?is)\bde\s+(.+?)\s*$""")

    override fun match(packageName: String?, title: String, body: String): Boolean =
        packageName == PACKAGE_NAME

    override fun parse(title: String, body: String, postedAt: Long): ParsedMovement? =
        parseMovement(title, body) { text ->
            firstGroup(payeeBeforeNequi, text)
                ?: firstGroup(payer, text)
                ?: firstGroup(payeeToEnd, text)
        }
}
