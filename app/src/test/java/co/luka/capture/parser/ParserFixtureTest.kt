package co.luka.capture.parser

import co.luka.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParserFixtureTest {
    @Test
    fun nequiFixtures() {
        val paid = NequiParser.parse("", "Pagaste $25.000 a Tienda X con tu Nequi", 0)
        assertEquals(Direction.EXPENSE, paid?.direction)
        assertEquals(25_000L, paid?.amount)
        assertEquals("Tienda X", paid?.counterparty)

        val received = NequiParser.parse("", "Recibiste $100.000 de Juan Pérez", 0)
        assertEquals(Direction.INCOME, received?.direction)
        assertEquals(100_000L, received?.amount)
        assertEquals("Juan Pérez", received?.counterparty)

        val sent = NequiParser.parse("", "Enviaste $50.000 a María López", 0)
        assertEquals(Direction.EXPENSE, sent?.direction)
        assertEquals(50_000L, sent?.amount)
        assertEquals("María López", sent?.counterparty)
    }

    @Test
    fun nuFixtures() {
        val purchase = NuParser.parse("", "Compra de $45.900 en RAPPI aprobada", 0)
        assertEquals(Direction.EXPENSE, purchase?.direction)
        assertEquals(45_900L, purchase?.amount)
        assertEquals("RAPPI", purchase?.counterparty)

        val transfer = NuParser.parse("", "Transferencia recibida por $200.000", 0)
        assertEquals(Direction.INCOME, transfer?.direction)
        assertEquals(200_000L, transfer?.amount)
        assertNull(transfer?.counterparty)
    }

    @Test
    fun bancolombiaFixtures() {
        val purchase = BancolombiaParser.parse("", "Compraste $32.500 en EXITO con tu tarjeta débito", 0)
        assertEquals(Direction.EXPENSE, purchase?.direction)
        assertEquals(32_500L, purchase?.amount)
        assertEquals("EXITO", purchase?.counterparty)

        val transfer = BancolombiaParser.parse("", "Recibiste una transferencia por $150.000 de CARLOS", 0)
        assertEquals(Direction.INCOME, transfer?.direction)
        assertEquals(150_000L, transfer?.amount)
        assertEquals("CARLOS", transfer?.counterparty)

        assertNull(BancolombiaParser.parse("", "Hoy es un buen día para ahorrar", 0))
    }

    @Test
    fun noticesStayInReview() {
        val notices = listOf(
            "Tu tarjeta débito tiene un cupo de $1.250.000",
            "Compra rechazada por $20.000",
            "Recuerda el pago de $80.000",
        )
        val parsers = listOf(NequiParser, NuParser, BancolombiaParser)
        notices.forEach { text ->
            parsers.forEach { parser ->
                assertNull(text, parser.parse("", text, 0))
            }
        }
    }

    @Test
    fun creditPaymentIsExpense() {
        val paid = NequiParser.parse("", "Abonaste $50.000 a tu crédito", 0)
        assertEquals(Direction.EXPENSE, paid?.direction)
        assertEquals(50_000L, paid?.amount)
    }

    @Test
    fun counterpartyStopsBeforeBalance() {
        val sent = NequiParser.parse("", "Enviaste $50.000 a María López. Tu saldo es $10.000", 0)
        assertEquals(Direction.EXPENSE, sent?.direction)
        assertEquals(50_000L, sent?.amount)
        assertEquals("María López", sent?.counterparty)

        val line = NequiParser.parse("", "Enviaste $50.000 a María López\nTu saldo es $10.000", 0)
        assertEquals("María López", line?.counterparty)

        val words = NequiParser.parse("", "Recibiste $100.000 de Juan Pérez tu saldo es $5.000", 0)
        assertEquals(Direction.INCOME, words?.direction)
        assertEquals(100_000L, words?.amount)
        assertEquals("Juan Pérez", words?.counterparty)
    }
}
