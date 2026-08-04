package fr.berrelesalpes.grc.ui.demarches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemarcheDetail
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemarcheDetailUiState(
    val dossier: DemarcheDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nouveauMessage: String = "",
    val isSendingMessage: Boolean = false,
)

class DemarcheDetailViewModel(
    private val repository: DemarcheRepository,
    private val demarcheId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DemarcheDetailUiState())
    val uiState: StateFlow<DemarcheDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.getDemarche(demarcheId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, dossier = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onMessageChange(value: String) {
        _uiState.value = _uiState.value.copy(nouveauMessage = value)
    }

    fun sendMessage() {
        val contenu = _uiState.value.nouveauMessage.trim()
        if (contenu.isBlank()) return

        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        viewModelScope.launch {
            when (val result = repository.addMessage(demarcheId, contenu)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false, nouveauMessage = "")
                    refresh() // Recharge le fil pour afficher le nouveau message avec son horodatage serveur.
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false, errorMessage = result.message)
                }
            }
        }
    }
}
