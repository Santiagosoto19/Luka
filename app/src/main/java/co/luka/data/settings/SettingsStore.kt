package co.luka.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "luka_settings")

data class UserSettings(
    val onboardingDone: Boolean = false,
    val theme: String = "system",
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val currency: String = "COP",
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.settingsDataStore
    private val alerts = stringSetPreferencesKey("budget_alerts")

    val settings: Flow<UserSettings> = store.data.map { prefs ->
        UserSettings(
            onboardingDone = prefs[booleanPreferencesKey("onboarding_done")] ?: false,
            theme = prefs[stringPreferencesKey("theme")] ?: "system",
            appLockEnabled = prefs[booleanPreferencesKey("app_lock_enabled")] ?: false,
            pinHash = prefs[stringPreferencesKey("pin_hash")] ?: "",
            pinSalt = prefs[stringPreferencesKey("pin_salt")] ?: "",
            currency = prefs[stringPreferencesKey("currency")] ?: "COP",
        )
    }

    suspend fun setOnboardingDone(done: Boolean) = edit(booleanPreferencesKey("onboarding_done"), done)
    suspend fun setTheme(theme: String) = edit(stringPreferencesKey("theme"), theme)
    suspend fun setLockEnabled(enabled: Boolean) = edit(booleanPreferencesKey("app_lock_enabled"), enabled)
    suspend fun setPin(hash: String, salt: String) {
        store.edit {
            it[stringPreferencesKey("pin_hash")] = hash
            it[stringPreferencesKey("pin_salt")] = salt
            it[booleanPreferencesKey("app_lock_enabled")] = true
        }
    }

    suspend fun hasBudgetAlert(key: String): Boolean = store.data.first()[alerts].orEmpty().contains(key)

    suspend fun rememberBudgetAlert(key: String) {
        store.edit { prefs ->
            prefs[alerts] = prefs[alerts].orEmpty() + key
        }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    private suspend fun <T> edit(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }
}
