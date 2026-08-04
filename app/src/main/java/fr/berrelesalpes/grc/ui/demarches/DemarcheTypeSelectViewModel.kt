package fr.berrelesalpes.grc.ui.demarches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemarcheType
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemarcheTypeSelectUiState(
    val types: List<DemarcheType> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class DemarcheTypeSelectViewModel(private val repository: DemarcheRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DemarcheTypeSelectUiState())
    val uiState: StateFlow<DemarcheTypeSelectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getTypes()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, types = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
