package fr.berrelesalpes.grc.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Fabrique générique minimaliste, pour construire des ViewModel prenant des
 * dépendances en paramètre de constructeur sans recourir à Hilt.
 */
class SimpleViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        return creator() as VM
    }
}
