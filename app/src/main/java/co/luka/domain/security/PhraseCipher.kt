package co.luka.domain.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PhraseCipher {
    private val magic = "LUKA1".toByteArray(Charsets.US_ASCII)

    fun encrypt(plain: ByteArray, phrase: CharArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(phrase, salt), "AES"), GCMParameterSpec(128, iv))
        return magic + salt + iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray, phrase: CharArray): ByteArray {
        if (blob.size < 5 + 16 + 12 + 16 || !blob.copyOfRange(0, 5).contentEquals(magic)) {
            throw IllegalArgumentException("El archivo no es una copia de Luka.")
        }
        val salt = blob.copyOfRange(5, 21)
        val iv = blob.copyOfRange(21, 33)
        val body = blob.copyOfRange(33, blob.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derive(phrase, salt), "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(body)
        } catch (error: GeneralSecurityException) {
            throw IllegalArgumentException("La frase no abre esta copia.", error)
        }
    }

    private fun derive(phrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(phrase, salt, 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
