package fr.berrelesalpes.grc.data.network

import com.squareup.moshi.Moshi
import fr.berrelesalpes.grc.data.model.ApiErrorBody
import retrofit2.Response

/**
 * Enveloppe le résultat d'un appel réseau, pour forcer l'appelant à gérer
 * explicitement le cas d'erreur plutôt que de laisser une exception remonter
 * jusqu'à l'interface (mauvaise expérience utilisateur garantie).
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null, val httpStatus: Int? = null) : ApiResult<Nothing>()
}

/**
 * Convertit une réponse Retrofit en [ApiResult], en extrayant le message
 * d'erreur lisible fourni par l'API WordPress (format WP_Error standard :
 * { "code": "...", "message": "...", "data": { "status": ... } }) plutôt que
 * d'afficher un code HTTP brut à l'utilisateur.
 */
fun <T> Response<T>.toApiResult(moshi: Moshi): ApiResult<T> {
    if (isSuccessful) {
        val body = body()
        return if (body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.Error("Réponse vide du serveur.", httpStatus = code())
        }
    }

    val errorJson = errorBody()?.string()
    val parsed = errorJson?.let {
        try {
            moshi.adapter(ApiErrorBody::class.java).fromJson(it)
        } catch (e: Exception) {
            null
        }
    }

    val message = parsed?.message ?: when (code()) {
        429 -> "Trop de tentatives. Merci de réessayer dans quelques instants."
        in 500..599 -> "Le service est momentanément indisponible. Merci de réessayer plus tard."
        else -> "Une erreur est survenue. Merci de réessayer."
    }

    return ApiResult.Error(message = message, code = parsed?.code, httpStatus = code())
}

/**
 * Enveloppe les exceptions non gérées par [toApiResult] (pas de connexion,
 * timeout, ou réponse serveur dans un format inattendu qui fait échouer
 * l'analyse JSON) dans le même type de résultat, pour ne jamais laisser une
 * exception non rattrapée faire planter l'application.
 */
fun <T> networkErrorResult(e: Exception): ApiResult<T> = ApiResult.Error(
    message = "Impossible de contacter le serveur ou réponse inattendue. Vérifiez votre connexion internet et réessayez."
)
