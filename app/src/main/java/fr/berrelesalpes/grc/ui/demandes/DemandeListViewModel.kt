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

data class DemandeListUiState(
    val demandes: List<DemandeSignalement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class DemandeListViewModel(private val repository: DemandeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DemandeListUiState())
    val uiState: StateFlow<DemandeListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.getMyDemandes()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, demandes = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
