package fr.berrelesalpes.grc.ui.common

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Fabrique générique minimaliste, pour construire des ViewModel prenant des
 * dépendances en paramètre de constructeur sans recourir à Hilt.
 *
 * Fournit un [SavedStateHandle] à chaque ViewModel créé — nécessaire pour
 * que l'état d'un écran survive à un cas fréquent sur Android : le système
 * peut détruire puis recréer le processus de l'application pendant qu'elle
 * est en arrière-plan (ex : appareil photo ouvert par-dessus, faible
 * mémoire disponible), ce qui recréerait un ViewModel totalement vierge
 * sans ce mécanisme. Les ViewModel n'en ayant pas besoin peuvent
 * simplement ignorer le paramètre reçu par le lambda créateur.
 */
class SimpleViewModelFactory<T : ViewModel>(private val creator: (SavedStateHandle) -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        val savedStateHandle = extras.createSavedStateHandle()
        return creator(savedStateHandle) as VM
    }
}
