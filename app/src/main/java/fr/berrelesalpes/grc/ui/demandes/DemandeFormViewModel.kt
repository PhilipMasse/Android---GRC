package fr.berrelesalpes.grc.ui.demandes

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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

class DemandeFormViewModel(private val repository: DemandeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DemandeFormUiState())
    val uiState: StateFlow<DemandeFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(categories = result.data)
                is ApiResult.Error -> { /* Catégorie facultative : un échec de chargement n'empêche pas de signaler. */ }
            }
        }
    }

    fun onTitreChange(value: String) {
        _uiState.value = _uiState.value.copy(titre = value, errorMessage = null)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, errorMessage = null)
    }

    fun onCategorieChange(id: Int?) {
        _uiState.value = _uiState.value.copy(categorieId = id)
    }

    fun onPhotosSelectionnees(uris: List<Uri>) {
        val s = _uiState.value
        _uiState.value = s.copy(photosSelectionnees = (s.photosSelectionnees + uris).distinct())
    }

    fun retirerPhoto(uri: Uri) {
        val s = _uiState.value
        _uiState.value = s.copy(photosSelectionnees = s.photosSelectionnees.filterNot { it == uri })
    }

    /** Positionnement manuel (clic/glisser sur la carte) : met à jour la position et relance la détection de doublons. */
    fun onPositionChoisie(context: Context, lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(latitude = lat, longitude = lng)
        rechercherAdresse(lat, lng)
        rechercherDoublons(lat, lng)
    }

    /** Géolocalisation automatique au lancement du formulaire (permission déjà vérifiée par l'écran appelant). */
    fun localiserAutomatiquement(context: Context) {
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
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(adresse = result.data.adresse)
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
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }
}
