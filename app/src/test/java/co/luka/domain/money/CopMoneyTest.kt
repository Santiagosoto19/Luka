package co.luka.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopMoneyTest {
    @Test
    fun parsesColombianAmountsAsLongPesos() {
        assertEquals(25_000L, CopMoney.parseFirst("$25.000"))
        assertEquals(1_250_000L, CopMoney.parseFirst("$1.250.000"))
        assertEquals(25_000L, CopMoney.parseFirst("$25.000,00"))
        assertEquals(25_000L, CopMoney.parseFirst("$ 25.000"))
        assertEquals(25_000L, CopMoney.parseFirst("25000"))
        assertEquals(45_900L, CopMoney.parseFirst("Compra de $45.900 en RAPPI aprobada"))
    }

    @Test
    fun prefersAmountNextToTheVerb() {
        assertEquals(20_000L, CopMoney.parseFirst("Saldo $1.250.000. Pagaste $20.000 en Tienda X"))
    }

    @Test
    fun ignoresTextWithoutAnAmount() {
        assertNull(CopMoney.parseFirst("Hoy es un buen día para ahorrar"))
    }

    @Test
    fun formatsWithThousandsDot() {
        assertEquals("$1.250.000", CopMoney.format(1_250_000))
        assertEquals("$25.000", CopMoney.format(25_000))
        assertEquals("$0", CopMoney.format(0))
        assertEquals("-$10.000", CopMoney.format(-10_000))
    }
}
