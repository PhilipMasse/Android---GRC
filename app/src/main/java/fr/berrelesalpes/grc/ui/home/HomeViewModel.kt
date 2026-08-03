package fr.berrelesalpes.grc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.Citoyen
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val citoyen: Citoyen? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val loggedOut: Boolean = false,
)

class HomeViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.getCurrentUser()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, citoyen = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = _uiState.value.copy(loggedOut = true)
    }
}
