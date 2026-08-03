package fr.berrelesalpes.grc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun submit() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Merci de renseigner votre email.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            when (val result = repository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
