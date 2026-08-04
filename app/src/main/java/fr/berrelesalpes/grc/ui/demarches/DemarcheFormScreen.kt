package fr.berrelesalpes.grc.ui.demarches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.data.model.ChampDemarche
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField
import fr.berrelesalpes.grc.ui.common.SuccessBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemarcheFormScreen(
    viewModel: DemarcheFormViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.type?.nom ?: "Nouvelle démarche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        when {
            state.isLoadingType -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.submittedNumero != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    SuccessBanner("Votre dossier ${state.submittedNumero} a bien été envoyé. Vous pouvez suivre son avancement depuis \"Mes démarches\".")
                    Spacer(Modifier.height(20.dp))
                    GrcPrimaryButton(text = "Retour à mes démarches", onClick = onSubmitted)
                }
            }
            state.type != null -> {
                val type = state.type!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!type.description.isNullOrBlank()) {
                        Text(text = type.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                    }

                    state.errorMessage?.let {
                        ErrorBanner(it)
                        Spacer(Modifier.height(16.dp))
                    }

                    type.champs.forEach { champ ->
                        DemarcheChampField(
                            champ = champ,
                            value = state.valeurs[champ.key] ?: "",
                            onValueChange = { viewModel.onValueChange(champ.key, it) },
                            enabled = !state.isSubmitting,
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    GrcPrimaryButton(
                        text = "Envoyer le dossier",
                        onClick = viewModel::submit,
                        isLoading = state.isSubmitting,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    state.errorMessage?.let { ErrorBanner(it) }
                }
            }
        }
    }
}

@Composable
private fun DemarcheChampField(
    champ: ChampDemarche,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    if (champ.type == "file") {
        // Les pièces jointes ne sont pas encore prises en charge par cette
        // version de l'application — le champ est signalé plutôt que masqué
        // silencieusement, pour que le citoyen sache qu'il devra utiliser le
        // site web pour joindre ce document.
        Column {
            Text(
                text = champ.label + if (champ.requis) " *" else "",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "L'ajout de pièce jointe n'est pas encore disponible sur l'application. Utilisez le site web pour ce document.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB32D2E),
            )
        }
        return
    }

    val keyboardType = when (champ.type) {
        "email" -> KeyboardType.Email
        "number" -> KeyboardType.Number
        "phone" -> KeyboardType.Phone
        "date" -> KeyboardType.Number // Saisie AAAA-MM-JJ ; pas de sélecteur de date dans ce premier lot.
        else -> KeyboardType.Text
    }

    val suffixeObligatoire = if (champ.requis) " *" else ""
    val suffixeFormat = if (champ.type == "date") " (AAAA-MM-JJ)" else ""
    val label = champ.label + suffixeObligatoire + suffixeFormat

    GrcTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardType = keyboardType,
        enabled = enabled,
    )
}
