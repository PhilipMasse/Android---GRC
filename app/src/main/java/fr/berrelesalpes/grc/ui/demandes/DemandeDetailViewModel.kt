package fr.berrelesalpes.grc.ui.demandes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemandeSignalement
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.DemandeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemandeDetailUiState(
    val demande: DemandeSignalement? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * L'API ne propose pas de route de détail accessible aux citoyens
 * authentifiés par jeton (GET /demandes/{id} exige une session WordPress
 * classique, réservée aux agents) — le détail est donc obtenu en filtrant
 * la liste complète, qui contient déjà toutes les informations utiles (voir
 * GRC_REST_Demandes::format_demande_public côté plugin).
 */
class DemandeDetailViewModel(
    private val repository: DemandeRepository,
    private val demandeId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DemandeDetailUiState())
    val uiState: StateFlow<DemandeDetailUiState> = _uiState.asStateFlow()

    init {
        charger()
    }

    fun charger() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.getMyDemandes()) {
                is ApiResult.Success -> {
                    val demande = result.data.firstOrNull { it.id == demandeId }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        demande = demande,
                        errorMessage = if (demande == null) "Signalement introuvable." else null,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
