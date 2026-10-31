package co.luka.domain.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun blankSaltNeverUnlocks() {
        assertFalse(PinHasher.isConfigured("", ""))
        assertFalse(PinHasher.isConfigured("", "hash"))
        assertFalse(PinHasher.isConfigured("salt", ""))
        assertFalse(PinHasher.accepts("1234", "", ""))
        assertFalse(PinHasher.accepts("0000", "", "hash"))
    }

    @Test
    fun savedPinUnlocksOnlyThatPin() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.isConfigured(salt, hash))
        assertTrue(PinHasher.accepts("1234", salt, hash))
        assertFalse(PinHasher.accepts("1235", salt, hash))
        assertFalse(PinHasher.accepts("1234", "", hash))
    }
}
