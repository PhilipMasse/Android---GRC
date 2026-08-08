package fr.berrelesalpes.grc.data.network

import fr.berrelesalpes.grc.data.model.AddMessageResponse
import fr.berrelesalpes.grc.data.model.CaptchaChallenge
import fr.berrelesalpes.grc.data.model.Categorie
import fr.berrelesalpes.grc.data.model.Citoyen
import fr.berrelesalpes.grc.data.model.DemandeProche
import fr.berrelesalpes.grc.data.model.DemandeSignalement
import fr.berrelesalpes.grc.data.model.DemarcheDetail
import fr.berrelesalpes.grc.data.model.DemarcheResume
import fr.berrelesalpes.grc.data.model.DemarcheType
import fr.berrelesalpes.grc.data.model.ForgotPasswordRequest
import fr.berrelesalpes.grc.data.model.LoginRequest
import fr.berrelesalpes.grc.data.model.LoginResponse
import fr.berrelesalpes.grc.data.model.MessageResponse
import fr.berrelesalpes.grc.data.model.RefreshRequest
import fr.berrelesalpes.grc.data.model.RefreshResponse
import fr.berrelesalpes.grc.data.model.RegisterRequest
import fr.berrelesalpes.grc.data.model.ResetPasswordRequest
import fr.berrelesalpes.grc.data.model.ReverseGeocodeResponse
import fr.berrelesalpes.grc.data.model.SubmitDemandeRequest
import fr.berrelesalpes.grc.data.model.SubmitDemandeResponse
import fr.berrelesalpes.grc.data.model.SubmitDemarcheRequest
import fr.berrelesalpes.grc.data.model.SubmitDemarcheResponse
import fr.berrelesalpes.grc.data.model.TokenResponse
import fr.berrelesalpes.grc.data.model.TwoFactorVerifyRequest
import fr.berrelesalpes.grc.data.model.PieceJointeUploadResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Décrit les routes de /wp-json/grc/v1/ consommées par l'application.
 * Voir includes/class-grc-rest-api.php et le dossier includes/rest/ côté
 * plugin WordPress pour la définition faisant foi de chaque route.
 */
interface GrcApiService {

    @POST("citoyen/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("citoyen/2fa/verifier")
    suspend fun verifyTwoFactor(@Body body: TwoFactorVerifyRequest): Response<TokenResponse>

    @POST("citoyen/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST("citoyen/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @GET("captcha")
    suspend fun getCaptcha(): Response<CaptchaChallenge>

    @POST("citoyen/mot-de-passe-oublie")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<MessageResponse>

    @POST("citoyen/reinitialiser-mot-de-passe")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<MessageResponse>

    @GET("citoyen/me")
    suspend fun getMe(): Response<Citoyen>

    // --- Démarches administratives ---------------------------------------

    @GET("demarches/types")
    suspend fun getDemarcheTypes(): Response<List<DemarcheType>>

    @POST("demarches")
    suspend fun submitDemarche(@Body body: SubmitDemarcheRequest): Response<SubmitDemarcheResponse>

    @GET("mes-demarches")
    suspend fun getMyDemarches(): Response<List<DemarcheResume>>

    @GET("demarches/{id}")
    suspend fun getDemarche(@Path("id") id: Int): Response<DemarcheDetail>

    /**
     * Formulaire encodé (et non JSON) : c'est ce qu'attend le contrôleur
     * WordPress (get_param('contenu')), qui accepte aussi bien un envoi
     * multipart (avec pièces jointes, non géré dans ce premier lot) qu'un
     * simple formulaire encodé pour un message texte seul.
     */
    @FormUrlEncoded
    @POST("demarches/{id}/messages")
    suspend fun addDemarcheMessage(
        @Path("id") id: Int,
        @Field("contenu") contenu: String,
    ): Response<AddMessageResponse>

    /** Variante multipart, utilisée quand le message est accompagné d'au moins un fichier. */
    @Multipart
    @POST("demarches/{id}/messages")
    suspend fun addDemarcheMessageWithFiles(
        @Path("id") id: Int,
        @Part("contenu") contenu: RequestBody,
        @Part files: List<MultipartBody.Part>,
    ): Response<AddMessageResponse>

    @Multipart
    @POST("demarches/{id}/pieces-jointes")
    suspend fun uploadDemarchePieces(
        @Path("id") id: Int,
        @Part files: List<MultipartBody.Part>,
    ): Response<List<PieceJointeUploadResult>>

    // --- Signalements -------------------------------------------------

    @GET("categories")
    suspend fun getCategories(): Response<List<Categorie>>

    @POST("demandes/public-submit")
    suspend fun submitDemande(@Body body: SubmitDemandeRequest): Response<SubmitDemandeResponse>

    @GET("mes-demandes")
    suspend fun getMyDemandes(): Response<List<DemandeSignalement>>

    @GET("demandes/proches")
    suspend fun getDemandesProches(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<DemandeProche>>

    @GET("geocode/reverse")
    suspend fun reverseGeocode(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<ReverseGeocodeResponse>

    @Multipart
    @POST("demandes/{id}/pieces-jointes")
    suspend fun uploadDemandePieces(
        @Path("id") id: Int,
        @Part files: List<MultipartBody.Part>,
    ): Response<List<PieceJointeUploadResult>>
}
