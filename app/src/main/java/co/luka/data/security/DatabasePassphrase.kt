package co.luka.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabasePassphrase @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences: SharedPreferences = open(context)

    fun getOrCreate(): ByteArray {
        val existing = preferences.getString(KEY, null)
        if (existing != null) return Base64.getDecoder().decode(existing)
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // commit() termina de escribir antes de abrir SQLCipher. apply() puede perder la clave si el proceso muere.
        val saved = preferences.edit().putString(KEY, Base64.getEncoder().encodeToString(bytes)).commit()
        check(saved) { "No se pudo guardar la clave de la base." }
        return bytes
    }

    private fun open(context: Context): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            FILE,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE = "luka_sqlcipher"
        const val KEY = "passphrase"
    }
}
