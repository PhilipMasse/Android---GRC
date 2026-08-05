package fr.berrelesalpes.grc.data.repository

import com.squareup.moshi.Moshi
import fr.berrelesalpes.grc.data.model.AddMessageResponse
import fr.berrelesalpes.grc.data.model.DemarcheDetail
import fr.berrelesalpes.grc.data.model.DemarcheResume
import fr.berrelesalpes.grc.data.model.DemarcheType
import fr.berrelesalpes.grc.data.model.SubmitDemarcheRequest
import fr.berrelesalpes.grc.data.model.SubmitDemarcheResponse
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.GrcApiService
import fr.berrelesalpes.grc.data.network.networkErrorResult
import fr.berrelesalpes.grc.data.network.toApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Point d'entrée unique pour toutes les opérations liées aux démarches
 * administratives. Le citoyen étant toujours connecté dans cette
 * application (contrairement au portail web qui autorise un mode invité
 * protégé par captcha), aucune gestion de captcha n'est nécessaire ici :
 * le jeton JWT identifie déjà le citoyen côté serveur.
 */
class DemarcheRepository(private val api: GrcApiService) {

    private val moshi = Moshi.Builder().build()

    suspend fun getTypes(): ApiResult<List<DemarcheType>> = withContext(Dispatchers.IO) {
        try {
            api.getDemarcheTypes().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun submit(typeSlug: String, donnees: Map<String, String>): ApiResult<SubmitDemarcheResponse> =
        withContext(Dispatchers.IO) {
            try {
                api.submitDemarche(SubmitDemarcheRequest(typeSlug, donnees)).toApiResult(moshi)
            } catch (e: Exception) {
                networkErrorResult(e)
            }
        }

    suspend fun getMyDemarches(): ApiResult<List<DemarcheResume>> = withContext(Dispatchers.IO) {
        try {
            api.getMyDemarches().toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun getDemarche(id: Int): ApiResult<DemarcheDetail> = withContext(Dispatchers.IO) {
        try {
            api.getDemarche(id).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun addMessage(demarcheId: Int, contenu: String): ApiResult<AddMessageResponse> = withContext(Dispatchers.IO) {
        try {
            api.addDemarcheMessage(demarcheId, contenu).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    suspend fun addMessageWithFiles(
        demarcheId: Int,
        contenu: String,
        fichiers: List<MultipartBody.Part>,
    ): ApiResult<AddMessageResponse> = withContext(Dispatchers.IO) {
        try {
            val contenuBody = contenu.toRequestBody("text/plain".toMediaTypeOrNull())
            api.addDemarcheMessageWithFiles(demarcheId, contenuBody, fichiers).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }

    /**
     * Envoie une ou plusieurs pièces jointes vers un dossier déjà créé.
     * Chaque fichier est traité indépendamment côté serveur : un échec sur
     * l'un d'eux n'empêche pas les autres d'être acceptés (voir la réponse
     * détaillée par fichier dans [PieceJointeUploadResult]).
     */
    suspend fun uploadPieces(
        demarcheId: Int,
        fichiers: List<MultipartBody.Part>,
    ): ApiResult<List<PieceJointeUploadResult>> = withContext(Dispatchers.IO) {
        try {
            api.uploadDemarchePieces(demarcheId, fichiers).toApiResult(moshi)
        } catch (e: Exception) {
            networkErrorResult(e)
        }
    }
}
