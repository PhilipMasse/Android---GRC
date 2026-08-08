package fr.berrelesalpes.grc.data.repository

import com.squareup.moshi.Moshi
import fr.berrelesalpes.grc.data.model.Categorie
import fr.berrelesalpes.grc.data.model.DemandeProche
import fr.berrelesalpes.grc.data.model.DemandeSignalement
import fr.berrelesalpes.grc.data.model.PieceJointeUploadResult
import fr.berrelesalpes.grc.data.model.ReverseGeocodeResponse
import fr.berrelesalpes.grc.data.model.SubmitDemandeRequest
import fr.berrelesalpes.grc.data.model.SubmitDemandeResponse
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.GrcApiService
import fr.berrelesalpes.grc.data.network.networkErrorResult
import fr.berrelesalpes.grc.data.network.toApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

/** Point d'entrée unique pour toutes les opérations liées aux signalements. */
class DemandeRepository(private val api: GrcApiService) {

    private val moshi = Moshi.Builder().build()

    suspend fun getCategories(): ApiResult<List<Categorie>> = withContext(Dispatchers.IO) {
        try {
            api.getCategories().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun submit(request: SubmitDemandeRequest): ApiResult<SubmitDemandeResponse> = withContext(Dispatchers.IO) {
        try {
            api.submitDemande(request).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun getMyDemandes(): ApiResult<List<DemandeSignalement>> = withContext(Dispatchers.IO) {
        try {
            api.getMyDemandes().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun getDemandesProches(lat: Double, lng: Double): ApiResult<List<DemandeProche>> = withContext(Dispatchers.IO) {
        try {
            api.getDemandesProches(lat, lng).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): ApiResult<ReverseGeocodeResponse> = withContext(Dispatchers.IO) {
        try {
            api.reverseGeocode(lat, lng).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun uploadPieces(demandeId: Int, fichiers: List<MultipartBody.Part>): ApiResult<List<PieceJointeUploadResult>> =
        withContext(Dispatchers.IO) {
            try {
                api.uploadDemandePieces(demandeId, fichiers).toApiResult(moshi)
            } catch (e: Exception) {
                networkErrorResult(e)
            }
        }
}
