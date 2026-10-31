package co.luka.capture.parser

import javax.inject.Inject
import javax.inject.Singleton

// Un banco nuevo es un archivo MovementParser, una línea en esta lista y una fila en sources.
@Singleton
class ParserRegistry @Inject constructor() {
    val parsers: List<MovementParser> = listOf(NequiParser, NuParser, BancolombiaParser)

    fun byId(id: String?): MovementParser? = parsers.firstOrNull { it.id == id }

    fun forPackage(packageName: String?): MovementParser? =
        parsers.firstOrNull { it.match(packageName, "", "") }
}
