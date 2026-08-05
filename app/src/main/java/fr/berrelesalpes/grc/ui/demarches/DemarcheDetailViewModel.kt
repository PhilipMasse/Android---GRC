package fr.berrelesalpes.grc.ui.demarches

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berrelesalpes.grc.data.model.DemarcheDetail
import fr.berrelesalpes.grc.data.network.ApiResult
import fr.berrelesalpes.grc.data.network.MultipartFileHelper
import fr.berrelesalpes.grc.data.repository.DemarcheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DemarcheDetailUiState(
    val dossier: DemarcheDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nouveauMessage: String = "",
    val fichiersMessage: List<Uri> = emptyList(),
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

    fun onFichiersSelectionnes(uris: List<Uri>) {
        val s = _uiState.value
        _uiState.value = s.copy(fichiersMessage = (s.fichiersMessage + uris).distinct())
    }

    fun retirerFichier(uri: Uri) {
        val s = _uiState.value
        _uiState.value = s.copy(fichiersMessage = s.fichiersMessage.filterNot { it == uri })
    }

    fun sendMessage(contentResolver: ContentResolver, cacheDir: File) {
        val state = _uiState.value
        val contenu = state.nouveauMessage.trim()
        if (contenu.isBlank() && state.fichiersMessage.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSendingMessage = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (state.fichiersMessage.isEmpty()) {
                repository.addMessage(demarcheId, contenu)
            } else {
                val parts = MultipartFileHelper.toMultipartParts(contentResolver, state.fichiersMessage, cacheDir)
                repository.addMessageWithFiles(demarcheId, contenu, parts)
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        nouveauMessage = "",
                        fichiersMessage = emptyList(),
                    )
                    refresh() // Recharge le fil pour afficher le nouveau message avec son horodatage serveur.
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false, errorMessage = result.message)
                }
            }
        }
    }
}
