package fr.berrelesalpes.grc.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ces classes reflètent exactement les formes de requêtes/réponses de l'API
 * REST du plugin GRC (voir includes/rest/class-grc-rest-citoyen.php côté
 * WordPress). Toute évolution de l'API côté serveur doit être répercutée ici.
 */

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Réponse de POST /citoyen/login — deux formes possibles selon que la double
 * authentification est activée ou non pour ce compte. Les deux jeux de
 * champs sont optionnels ici ; le repository distingue les cas via
 * [requiresTwoFactor].
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    // Présents si la connexion est immédiate (pas de 2FA) :
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null,
    @Json(name = "citoyen_id") val citoyenId: Int? = null,
    // Présents si une seconde étape (2FA) est requise :
    @Json(name = "requires_2fa") val requiresTwoFactor: Boolean? = null,
    val method: String? = null, // "email" ou "totp"
    @Json(name = "pending_token") val pendingToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class TwoFactorVerifyRequest(
    @Json(name = "pending_token") val pendingToken: String,
    val code: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "citoyen_id") val citoyenId: Int,
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val prenom: String,
    val nom: String,
    val email: String,
    val password: String,
    @Json(name = "site_web") val siteWeb: String = "", // honeypot — doit toujours rester vide
    @Json(name = "captcha_token") val captchaToken: String? = null,
    @Json(name = "captcha_reponse") val captchaReponse: String? = null,
    @Json(name = "captcha_provider_token") val captchaProviderToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class CaptchaChallenge(
    val token: String,
    val question: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val token: String,
    @Json(name = "mot_de_passe") val motDePasse: String
)

@JsonClass(generateAdapter = true)
data class MessageResponse(val message: String)

@JsonClass(generateAdapter = true)
data class Citoyen(
    val id: Int,
    val nom: String?,
    val prenom: String?,
    val email: String?,
    val telephone: String? = null,
    @Json(name = "two_factor_method") val twoFactorMethod: String? = null,
)

/** Forme standard d'une erreur renvoyée par l'API WordPress (WP_Error). */
@JsonClass(generateAdapter = true)
data class ApiErrorBody(
    val code: String?,
    val message: String?,
)
