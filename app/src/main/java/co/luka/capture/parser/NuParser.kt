package co.luka.capture.parser

object NuParser : MovementParser {
    const val PACKAGE_NAME = "com.nu.production"
    override val id: String = "nu"

    private val merchant = Regex("""(?i)\ben\s+(.+?)\s+aprobada\b""")

    override fun match(packageName: String?, title: String, body: String): Boolean =
        packageName == PACKAGE_NAME

    override fun parse(title: String, body: String, postedAt: Long): ParsedMovement? =
        parseMovement(title, body) { text -> firstGroup(merchant, text) }
}
