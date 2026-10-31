package co.luka.domain.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), 50_000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return Base64.getEncoder().withoutPadding().encodeToString(key.encoded)
    }

    fun verify(pin: String, salt: String, expected: String): Boolean = hash(pin, salt) == expected

    fun isConfigured(salt: String, hash: String): Boolean = salt.isNotBlank() && hash.isNotBlank()

    fun accepts(pin: String, salt: String, hash: String): Boolean {
        if (!isConfigured(salt, hash)) return false
        return verify(pin, salt, hash)
    }
}
