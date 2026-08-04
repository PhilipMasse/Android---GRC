package fr.berrelesalpes.grc.ui.demarches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.ChampDemarche
import fr.berrelesalpes.grc.data.model.DemarcheType
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemarcheFormUiState(
    val type: DemarcheType? = null,
    val valeurs: Map<String, String> = emptyMap(),
    val isLoadingType: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
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

    fun submit() {
        val state = _uiState.value
        val type = state.type ?: return

        // Validation locale des champs obligatoires — l'API revalide de toute
        // façon côté serveur, mais un retour immédiat évite un aller-retour
        // réseau pour une erreur de saisie évidente.
        val champManquant = type.champs.firstOrNull { champ ->
            champ.requis && state.valeurs[champ.key].isNullOrBlank()
        }
        if (champManquant != null) {
            _uiState.value = state.copy(errorMessage = "Le champ \"${champManquant.label}\" est obligatoire.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            // Les champs de type "file" ne sont pas encore pris en charge par
            // cette version de l'application (voir README) — on les exclut de
            // l'envoi plutôt que de bloquer toute la soumission.
            val donnees = state.valeurs.filterKeys { key ->
                type.champs.firstOrNull { it.key == key }?.type != "file"
            }

            when (val result = repository.submit(type.slug, donnees)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedNumero = result.data.numeroDossier ?: "votre dossier"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }
}

/** Champs supportés par le formulaire dynamique dans cette version de l'application. */
fun ChampDemarche.isSupported(): Boolean = type != "file"
