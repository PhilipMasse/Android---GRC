package fr.berrelesalpes.grc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TwoFactorUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val verified: Boolean = false,
)

class TwoFactorViewModel(
    private val repository: AuthRepository,
    private val pendingToken: String,
    val method: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        // Le code fait toujours 6 chiffres (email ou TOTP) : on filtre la
        // saisie pour éviter les erreurs de frappe évidentes.
        val filtered = value.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(code = filtered, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.code.length != 6) {
            _uiState.value = state.copy(errorMessage = "Le code doit contenir 6 chiffres.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.verifyTwoFactor(pendingToken, state.code)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, verified = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
