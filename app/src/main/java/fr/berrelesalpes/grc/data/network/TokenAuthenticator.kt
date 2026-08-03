package fr.berrelesalpes.grc.data.network

import com.squareup.moshi.Moshi
import fr.berrelesalpes.grc.data.local.TokenManager
import fr.berrelesalpes.grc.data.model.RefreshRequest
import fr.berrelesalpes.grc.data.model.RefreshResponse
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * Se déclenche automatiquement quand le serveur répond 401 (jeton d'accès
 * expiré ou invalide) : tente un rafraîchissement via le refresh token, puis
 * rejoue la requête d'origine avec le nouveau jeton d'accès.
 *
 * Fonctionne en synchrone (bloquant) car c'est le contrat imposé par
 * l'interface OkHttp Authenticator — c'est normal ici : cet appel se produit
 * déjà sur le thread réseau d'OkHttp, jamais sur le thread principal.
 *
 * Utilise un OkHttpClient nu (sans AuthInterceptor ni ce même Authenticator)
 * pour l'appel de rafraîchissement lui-même, afin d'éviter toute boucle.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : Authenticator {

    private val moshi = Moshi.Builder().build()
    private val plainClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Évite une boucle infinie si le rafraîchissement lui-même échoue avec 401.
        if (responseCount(response) >= 2) {
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        val newAccessToken = synchronized(this) {
            // Un autre thread a peut-être déjà rafraîchi entre-temps (plusieurs
            // requêtes en vol simultanément) : on réutilise ce jeton si c'est le cas.
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != requestToken) {
                currentToken
            } else {
                performRefresh(refreshToken)
            }
        } ?: run {
            // Le refresh token est lui-même invalide/expiré : la session est terminée.
            tokenManager.clearSession()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun performRefresh(refreshToken: String): String? {
        return try {
            val adapter = moshi.adapter(RefreshRequest::class.java)
            val bodyJson = adapter.toJson(RefreshRequest(refreshToken))
            val request = Request.Builder()
                .url(baseUrl + "citoyen/refresh")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            plainClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = resp.body?.string() ?: return null
                val refreshResponse = moshi.adapter(RefreshResponse::class.java).fromJson(json) ?: return null
                tokenManager.updateAccessToken(refreshResponse.accessToken, refreshResponse.expiresIn)
                refreshResponse.accessToken
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
