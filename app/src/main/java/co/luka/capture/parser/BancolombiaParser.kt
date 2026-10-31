package co.luka.capture.parser

object BancolombiaParser : MovementParser {
    const val PACKAGE_NAME = "com.bancolombia.bancolombiaapp"
    override val id: String = "bancolombia"

    private val merchant = Regex("""(?i)\ben\s+(.+?)\s+con tu\b""")
    private val payer = Regex("""(?is)\bde\s+(.+?)\s*$""")

    override fun match(packageName: String?, title: String, body: String): Boolean =
        packageName == PACKAGE_NAME

    override fun parse(title: String, body: String, postedAt: Long): ParsedMovement? =
        parseMovement(title, body) { text ->
            firstGroup(merchant, text) ?: firstGroup(payer, text)
        }
}
