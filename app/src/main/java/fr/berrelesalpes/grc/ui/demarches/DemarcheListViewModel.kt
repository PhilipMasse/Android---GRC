package fr.berrelesalpes.grc.ui.demarches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemarcheResume
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DemarcheListUiState(
    val demarches: List<DemarcheResume> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class DemarcheListViewModel(private val repository: DemarcheRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DemarcheListUiState())
    val uiState: StateFlow<DemarcheListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.getMyDemarches()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, demarches = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
