package fr.berrelesalpes.grc.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.RegisterRequest
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val prenom: String = "",
    val nom: String = "",
    val email: String = "",
    val password: String = "",
    val captchaQuestion: String = "Chargement…",
    val captchaToken: String = "",
    val captchaReponse: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registered: Boolean = false,
)

/**
 * Gère uniquement le captcha "interne" (mathématique, auto-hébergé). Si la
 * mairie a activé un fournisseur tiers (Turnstile/reCAPTCHA/hCaptcha) côté
 * réglages du plugin, ce flux nécessitera l'intégration du SDK correspondant
 * — hors périmètre de ce premier lot, volontairement limité au captcha par
 * défaut du plugin.
 */
class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        loadCaptcha()
    }

    fun loadCaptcha() {
        viewModelScope.launch {
            when (val result = repository.getCaptcha()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        captchaQuestion = result.data.question,
                        captchaToken = result.data.token,
                        captchaReponse = "",
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(captchaQuestion = "Erreur de chargement — réessayez.")
                }
            }
        }
    }

    fun onFieldChange(prenom: String? = null, nom: String? = null, email: String? = null, password: String? = null, captchaReponse: String? = null) {
        val s = _uiState.value
        _uiState.value = s.copy(
            prenom = prenom ?: s.prenom,
            nom = nom ?: s.nom,
            email = email ?: s.email,
            password = password ?: s.password,
            captchaReponse = captchaReponse ?: s.captchaReponse,
            errorMessage = null,
        )
    }

    fun submit() {
        val s = _uiState.value
        if (s.prenom.isBlank() || s.nom.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Merci de renseigner tous les champs.")
            return
        }
        if (s.password.length < 8) {
            _uiState.value = s.copy(errorMessage = "Le mot de passe doit contenir au moins 8 caractères.")
            return
        }
        if (s.captchaReponse.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Merci de répondre à la vérification anti-robot.")
            return
        }

        _uiState.value = s.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val request = RegisterRequest(
                prenom = s.prenom.trim(),
                nom = s.nom.trim(),
                email = s.email.trim(),
                password = s.password,
                captchaToken = s.captchaToken,
                captchaReponse = s.captchaReponse.trim(),
            )
            when (val result = repository.register(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, registered = true)
                }
                is ApiResult.Error -> {
                    // La réponse attendue a pu expirer (usage unique) : on en
                    // recharge une nouvelle pour que le citoyen puisse
                    // réessayer immédiatement sans quitter l'écran.
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                    loadCaptcha()
                }
            }
        }
    }
}
