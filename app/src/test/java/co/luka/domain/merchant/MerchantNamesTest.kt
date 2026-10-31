package co.luka.domain.merchant

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNamesTest {
    @Test
    fun foldsCaseAccentsAndSpaces() {
        assertEquals("jose perez", normalizeMerchant("José   Pérez"))
        assertEquals("rappi", normalizeMerchant("  RAPPI "))
        assertEquals("tienda x", normalizeMerchant("Tienda X"))
        assertEquals("exito", normalizeMerchant("ÉXITO"))
    }
}
