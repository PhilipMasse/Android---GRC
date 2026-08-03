package fr.berrelesalpes.grc.data.network

import fr.berrelesalpes.grc.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ajoute automatiquement l'en-tête Authorization: Bearer <jeton> sur chaque
 * requête, lorsqu'un jeton d'accès est disponible. Les routes publiques
 * (login, register, captcha...) n'en ont pas besoin mais l'en-tête
 * supplémentaire ne les gêne pas.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val accessToken = tokenManager.getAccessToken()

        val request = if (accessToken != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
