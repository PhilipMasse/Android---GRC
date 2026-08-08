package fr.berrelesalpes.grc.ui.demarches

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemarcheType
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.MultipartFileHelper
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DemarcheFormUiState(
    val type: DemarcheType? = null,
    val valeurs: Map<String, String> = emptyMap(),
    val fichiersSelectionnes: List<Uri> = emptyList(),
    val isLoadingType: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val avertissementFichiers: String? = null,
    val submittedNumero: String? = null,
)

class DemarcheFormViewModel(
    private val repository: DemarcheRepository,
    private val typeSlug: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DemarcheFormUiState())
    val uiState: StateFlow<DemarcheFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getTypes()) {
                is ApiResult.Success -> {
                    val type = result.data.firstOrNull { it.slug == typeSlug }
                    if (type == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingType = false,
                            errorMessage = "Ce type de démarche n'est plus disponible."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingType = false, type = type)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingType = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onValueChange(key: String, value: String) {
        val s = _uiState.value
        _uiState.value = s.copy(valeurs = s.valeurs + (key to value), errorMessage = null)
    }

    fun onFichiersSelectionnes(uris: List<Uri>) {
        val s = _uiState.value
        // Limite raisonnable côté application (le serveur applique de toute
        // façon sa propre limite de taille par fichier, 8 Mo).
        val total = (s.fichiersSelectionnes + uris).distinct()
        _uiState.value = s.copy(fichiersSelectionnes = total, errorMessage = null)
    }

    fun retirerFichier(uri: Uri) {
        val s = _uiState.value
        _uiState.value = s.copy(fichiersSelectionnes = s.fichiersSelectionnes.filterNot { it == uri })
    }

    fun submit(contentResolver: ContentResolver, cacheDir: File) {
        val state = _uiState.value
        val type = state.type ?: return

        // Validation locale des champs obligatoires — l'API revalide de toute
        // façon côté serveur, mais un retour immédiat évite un aller-retour
        // réseau pour une erreur de saisie évidente. Un champ "file" requis
        // est considéré rempli dès qu'au moins un document a été sélectionné.
        val champManquant = type.champs.firstOrNull { champ ->
            if (!champ.requis) return@firstOrNull false
            if (champ.type == "file") state.fichiersSelectionnes.isEmpty()
            else state.valeurs[champ.key].isNullOrBlank()
        }
        if (champManquant != null) {
            _uiState.value = state.copy(errorMessage = "Le champ \"${champManquant.label}\" est obligatoire.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null, avertissementFichiers = null)
        viewModelScope.launch {
            // Les champs de type "file" doivent tout de même porter une valeur
            // non vide dans "donnees" : le serveur vérifie le caractère
            // obligatoire de TOUS les champs sur cette base, y compris les
            // fichiers (le contenu réel est envoyé séparément juste après la
            // création du dossier). On y place les noms des fichiers
            // sélectionnés, à l'identique du comportement du site web.
            val nomsFichiers = state.fichiersSelectionnes.joinToString(", ") { uri -> uri.lastPathSegment ?: "document" }
            val donnees = type.champs.mapNotNull { champ ->
                if (champ.type == "file") {
                    if (state.fichiersSelectionnes.isNotEmpty()) champ.key to nomsFichiers else null
                } else {
                    state.valeurs[champ.key]?.let { champ.key to it }
                }
            }.toMap()

            when (val result = repository.submit(type.slug, donnees)) {
                is ApiResult.Success -> {
                    val dossierId = result.data.id
                    if (state.fichiersSelectionnes.isEmpty() || dossierId == null) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submittedNumero = result.data.numeroDossier ?: "votre dossier"
                        )
                        return@launch
                    }

                    val parts = MultipartFileHelper.toMultipartParts(contentResolver, state.fichiersSelectionnes, cacheDir)
                    val uploadResult = repository.uploadPieces(dossierId, parts)
                    val avertissement = when (uploadResult) {
                        is ApiResult.Success -> {
                            val echecs = uploadResult.data.filter { it.error }
                            if (echecs.isEmpty()) null
                            else "Dossier envoyé, mais ${echecs.size} document(s) n'ont pas pu être joints : " +
                                echecs.joinToString("; ") { "${it.nomOriginal ?: "fichier"} (${it.message ?: "raison inconnue"})" }
                        }
                        is ApiResult.Error -> "Dossier envoyé, mais les documents n'ont pas pu être joints (${uploadResult.message})"
                    }

                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedNumero = result.data.numeroDossier ?: "votre dossier",
                        avertissementFichiers = avertissement,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }
}

