package fr.berrelesalpes.grc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class ResetPasswordViewModel(
    private val repository: AuthRepository,
    private val token: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, errorMessage = null)
    }

    fun submit() {
        val password = _uiState.value.newPassword
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "Le mot de passe doit contenir au moins 8 caractères.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.resetPassword(token, password)) {
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
