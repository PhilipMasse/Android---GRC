package fr.berrelesalpes.grc.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stocke les jetons d'authentification (accès + rafraîchissement) de façon
 * chiffrée sur l'appareil (Android Keystore via EncryptedSharedPreferences),
 * plutôt qu'en clair — équivalent mobile du chiffrement appliqué aux données
 * sensibles côté serveur.
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "grc_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _isLoggedIn = MutableStateFlow(hasValidSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveSession(accessToken: String, refreshToken: String, citoyenId: Int, expiresInSeconds: Int) {
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putInt(KEY_CITOYEN_ID, citoyenId)
            .putLong(KEY_EXPIRES_AT, expiresAtMillis)
            .apply()
        _isLoggedIn.value = true
    }

    fun updateAccessToken(accessToken: String, expiresInSeconds: Int) {
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, expiresAtMillis)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getCitoyenId(): Int = prefs.getInt(KEY_CITOYEN_ID, -1)

    /**
     * Le jeton d'accès expire au bout d'1 heure côté serveur. On considère
     * qu'il faut le rafraîchir un peu avant l'échéance réelle (marge de
     * sécurité de 60 secondes) pour éviter un aller-retour réseau inutile en
     * cas d'expiration pile au moment d'une requête.
     */
    fun isAccessTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return System.currentTimeMillis() >= (expiresAt - 60_000L)
    }

    private fun hasValidSession(): Boolean = getRefreshToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CITOYEN_ID = "citoyen_id"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
