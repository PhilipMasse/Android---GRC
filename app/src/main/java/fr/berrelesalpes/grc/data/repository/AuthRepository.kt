package fr.berrelesalpes.grc.data.repository

import com.squareup.moshi.Moshi
import fr.berrelesalpes.grc.data.local.TokenManager
import fr.berrelesalpes.grc.data.model.CaptchaChallenge
import fr.berrelesalpes.grc.data.model.Citoyen
import fr.berrelesalpes.grc.data.model.ForgotPasswordRequest
import fr.berrelesalpes.grc.data.model.LoginRequest
import fr.berrelesalpes.grc.data.model.RegisterRequest
import fr.berrelesalpes.grc.data.model.ResetPasswordRequest
import fr.berrelesalpes.grc.data.model.TwoFactorVerifyRequest
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.GrcApiService
import fr.berrelesalpes.grc.data.network.networkErrorResult
import fr.berrelesalpes.grc.data.network.toApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Point d'entrée unique pour toutes les opérations d'authentification
 * citoyenne. Les écrans (via leurs ViewModels) ne parlent jamais directement
 * à [GrcApiService] — toujours à travers ce repository, qui gère aussi la
 * persistance de la session dans [TokenManager].
 */
class AuthRepository(
    private val api: GrcApiService,
    private val tokenManager: TokenManager,
) {
    private val moshi = Moshi.Builder().build()

    /**
     * Tente une connexion. Le résultat distingue trois cas côté appelant :
     * succès immédiat (pas de 2FA), 2FA requise (voir [LoginOutcome]), ou erreur.
     */
    suspend fun login(email: String, password: String): ApiResult<LoginOutcome> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(email, password))
            when (val result = response.toApiResult(moshi)) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (body.requiresTwoFactor == true && body.pendingToken != null) {
                        ApiResult.Success(
                            LoginOutcome.RequiresTwoFactor(
                                pendingToken = body.pendingToken,
                                method = body.method ?: "email"
                            )
                        )
                    } else if (body.accessToken != null && body.refreshToken != null && body.citoyenId != null) {
                        persistSession(body.accessToken, body.refreshToken, body.citoyenId, body.expiresIn ?: 3600)
                        ApiResult.Success(LoginOutcome.LoggedIn)
                    } else {
                        ApiResult.Error("Réponse inattendue du serveur.")
                    }
                }
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun verifyTwoFactor(pendingToken: String, code: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.verifyTwoFactor(TwoFactorVerifyRequest(pendingToken, code))
            when (val result = response.toApiResult(moshi)) {
                is ApiResult.Success -> {
                    val body = result.data
                    persistSession(body.accessToken, body.refreshToken, body.citoyenId, body.expiresIn)
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun getCaptcha(): ApiResult<CaptchaChallenge> = withContext(Dispatchers.IO) {
        try {
            api.getCaptcha().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun register(request: RegisterRequest): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(request)
            when (val result = response.toApiResult(moshi)) {
                is ApiResult.Success -> {
                    val body = result.data
                    persistSession(body.accessToken, body.refreshToken, body.citoyenId, body.expiresIn)
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun forgotPassword(email: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            when (val result = api.forgotPassword(ForgotPasswordRequest(email)).toApiResult(moshi)) {
                is ApiResult.Success -> ApiResult.Success(result.data.message)
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            when (val result = api.resetPassword(ResetPasswordRequest(token, newPassword)).toApiResult(moshi)) {
                is ApiResult.Success -> ApiResult.Success(result.data.message)
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun getCurrentUser(): ApiResult<Citoyen> = withContext(Dispatchers.IO) {
        try {
            api.getMe().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    fun logout() {
        tokenManager.clearSession()
    }

    private fun persistSession(accessToken: String, refreshToken: String, citoyenId: Int, expiresIn: Int) {
        tokenManager.saveSession(accessToken, refreshToken, citoyenId, expiresIn)
    }
}

/** Distingue les deux issues possibles d'une tentative de connexion réussie côté API. */
sealed class LoginOutcome {
    data object LoggedIn : LoginOutcome()
    data class RequiresTwoFactor(val pendingToken: String, val method: String) : LoginOutcome()
}
