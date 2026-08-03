package fr.berrelesalpes.grc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import fr.berrelesalpes.grc.data.repository.LoginOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Non nul lorsque la 2FA doit être vérifiée avant de terminer la connexion.
    val pendingTwoFactor: PendingTwoFactor? = null,
    val loginSucceeded: Boolean = false,
)

data class PendingTwoFactor(val pendingToken: String, val method: String)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Veuillez renseigner votre email et votre mot de passe.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.login(state.email.trim(), state.password)) {
                is ApiResult.Success -> {
                    when (val outcome = result.data) {
                        is LoginOutcome.LoggedIn -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, loginSucceeded = true)
                        }
                        is LoginOutcome.RequiresTwoFactor -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                pendingTwoFactor = PendingTwoFactor(outcome.pendingToken, outcome.method)
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
