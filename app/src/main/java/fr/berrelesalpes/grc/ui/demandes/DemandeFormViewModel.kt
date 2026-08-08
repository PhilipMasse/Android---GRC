package fr.berrelesalpes.grc.ui.demandes

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.location.LocationHelper
import fr.berrelesalpes.grc.data.model.Categorie
import fr.berrelesalpes.grc.data.model.DemandeProche
import fr.berrelesalpes.grc.data.model.SubmitDemandeRequest
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.MultipartFileHelper
import fr.berrelesalpes.grc.data.repository.DemandeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DemandeFormUiState(
    val titre: String = "",
    val description: String = "",
    val categories: List<Categorie> = emptyList(),
    val categorieId: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val adresse: String? = null,
    val isLocalisationEnCours: Boolean = false,
    val photosSelectionnees: List<Uri> = emptyList(),
    val demandesProches: List<DemandeProche> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val avertissementPhotos: String? = null,
    val submittedNumero: String? = null,
)

/**
 * Reçoit un [SavedStateHandle] et y sauvegarde en continu les champs
 * saisis par le citoyen (titre, description, catégorie, position, photos).
 * Nécessaire car Android peut détruire puis recréer le processus de
 * l'application pendant qu'elle est en arrière-plan (par exemple lorsque le
 * citoyen prend une photo : l'application Appareil photo, gourmande en
 * mémoire, peut déclencher cela) — sans ce mécanisme, un ViewModel "normal"
 * repartirait de zéro et le formulaire en cours de saisie serait perdu.
 */
class DemandeFormViewModel(
    private val repository: DemandeRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DemandeFormUiState(
            titre = savedStateHandle.get<String>(CLE_TITRE) ?: "",
            description = savedStateHandle.get<String>(CLE_DESCRIPTION) ?: "",
            categorieId = savedStateHandle.get<Int>(CLE_CATEGORIE_ID),
            latitude = savedStateHandle.get<Double>(CLE_LATITUDE),
            longitude = savedStateHandle.get<Double>(CLE_LONGITUDE),
            adresse = savedStateHandle.get<String>(CLE_ADRESSE),
            photosSelectionnees = savedStateHandle.get<ArrayList<Uri>>(CLE_PHOTOS) ?: emptyList(),
        )
    )
    val uiState: StateFlow<DemandeFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(categories = result.data)
                is ApiResult.Error -> { /* Catégorie facultative : un échec de chargement n'empêche pas de signaler. */ }
            }
        }
    }

    /**
     * Applique la mise à jour à l'état, puis sauvegarde les champs critiques
     * (pas la totalité de l'état — les catégories chargées ou la liste des
     * doublons détectés seront simplement rechargées, inutile de les
     * persister) dans le SavedStateHandle.
     */
    private fun updateState(update: (DemandeFormUiState) -> DemandeFormUiState) {
        val nouveau = update(_uiState.value)
        _uiState.value = nouveau
        savedStateHandle[CLE_TITRE] = nouveau.titre
        savedStateHandle[CLE_DESCRIPTION] = nouveau.description
        savedStateHandle[CLE_CATEGORIE_ID] = nouveau.categorieId
        savedStateHandle[CLE_LATITUDE] = nouveau.latitude
        savedStateHandle[CLE_LONGITUDE] = nouveau.longitude
        savedStateHandle[CLE_ADRESSE] = nouveau.adresse
        savedStateHandle[CLE_PHOTOS] = ArrayList(nouveau.photosSelectionnees)
    }

    fun onTitreChange(value: String) {
        updateState { it.copy(titre = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        updateState { it.copy(description = value, errorMessage = null) }
    }

    fun onCategorieChange(id: Int?) {
        updateState { it.copy(categorieId = id) }
    }

    fun onPhotosSelectionnees(uris: List<Uri>) {
        updateState { it.copy(photosSelectionnees = (it.photosSelectionnees + uris).distinct()) }
    }

    fun retirerPhoto(uri: Uri) {
        updateState { it.copy(photosSelectionnees = it.photosSelectionnees.filterNot { p -> p == uri }) }
    }

    /** Positionnement manuel (clic/glisser sur la carte) : met à jour la position et relance la détection de doublons. */
    fun onPositionChoisie(context: Context, lat: Double, lng: Double) {
        updateState { it.copy(latitude = lat, longitude = lng) }
        rechercherAdresse(lat, lng)
        rechercherDoublons(lat, lng)
    }

    /**
     * Géolocalisation automatique au lancement du formulaire (permission déjà
     * vérifiée par l'écran appelant). Si une position a déjà été enregistrée
     * (retour après une recréation du processus), on ne l'écrase pas : la
     * dernière position choisie par le citoyen prévaut sur une nouvelle
     * lecture GPS.
     */
    fun localiserAutomatiquement(context: Context) {
        if (_uiState.value.latitude != null && _uiState.value.longitude != null) {
            return
        }
        _uiState.value = _uiState.value.copy(isLocalisationEnCours = true)
        viewModelScope.launch {
            val position = LocationHelper.obtenirPositionActuelle(context)
            _uiState.value = _uiState.value.copy(isLocalisationEnCours = false)
            if (position != null) {
                onPositionChoisie(context, position.latitude, position.longitude)
            }
        }
    }

    private fun rechercherAdresse(lat: Double, lng: Double) {
        viewModelScope.launch {
            when (val result = repository.reverseGeocode(lat, lng)) {
                is ApiResult.Success -> updateState { it.copy(adresse = result.data.adresse) }
                is ApiResult.Error -> { /* L'adresse est un simple confort d'affichage : on continue sans bloquer. */ }
            }
        }
    }

    private fun rechercherDoublons(lat: Double, lng: Double) {
        viewModelScope.launch {
            when (val result = repository.getDemandesProches(lat, lng)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(demandesProches = result.data)
                is ApiResult.Error -> { /* Non bloquant : simple avertissement d'aide, pas critique. */ }
            }
        }
    }

    fun submit(contentResolver: ContentResolver, cacheDir: File) {
        val state = _uiState.value
        if (state.titre.isBlank()) {
            _uiState.value = state.copy(errorMessage = "L'objet du signalement est obligatoire.")
            return
        }
        if (state.description.isBlank()) {
            _uiState.value = state.copy(errorMessage = "La description est obligatoire.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null, avertissementPhotos = null)
        viewModelScope.launch {
            val request = SubmitDemandeRequest(
                titre = state.titre.trim(),
                description = state.description.trim(),
                categorieId = state.categorieId,
                latitude = state.latitude,
                longitude = state.longitude,
                adresseLieu = state.adresse,
            )

            when (val result = repository.submit(request)) {
                is ApiResult.Success -> {
                    val demandeId = result.data.id
                    if (state.photosSelectionnees.isEmpty() || demandeId == null) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submittedNumero = result.data.numeroSuivi ?: "votre signalement"
                        )
                        effacerBrouillon()
                        return@launch
                    }

                    val parts = MultipartFileHelper.toMultipartParts(contentResolver, state.photosSelectionnees, cacheDir)
                    val uploadResult = repository.uploadPieces(demandeId, parts)
                    val avertissement = when (uploadResult) {
                        is ApiResult.Success -> {
                            val echecs = uploadResult.data.filter { it.error }
                            if (echecs.isEmpty()) null
                            else "Signalement envoyé, mais ${echecs.size} photo(s) n'ont pas pu être jointes : " +
                                echecs.joinToString("; ") { "${it.nomOriginal ?: "fichier"} (${it.message ?: "raison inconnue"})" }
                        }
                        is ApiResult.Error -> "Signalement envoyé, mais les photos n'ont pas pu être jointes (${uploadResult.message})"
                    }

                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedNumero = result.data.numeroSuivi ?: "votre signalement",
                        avertissementPhotos = avertissement,
                    )
                    effacerBrouillon()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Une fois le signalement envoyé avec succès, plus besoin de conserver le brouillon. */
    private fun effacerBrouillon() {
        listOf(CLE_TITRE, CLE_DESCRIPTION, CLE_CATEGORIE_ID, CLE_LATITUDE, CLE_LONGITUDE, CLE_ADRESSE, CLE_PHOTOS)
            .forEach { savedStateHandle.remove<Any>(it) }
    }

    companion object {
        private const val CLE_TITRE = "titre"
        private const val CLE_DESCRIPTION = "description"
        private const val CLE_CATEGORIE_ID = "categorieId"
        private const val CLE_LATITUDE = "latitude"
        private const val CLE_LONGITUDE = "longitude"
        private const val CLE_ADRESSE = "adresse"
        private const val CLE_PHOTOS = "photos"
    }
}
