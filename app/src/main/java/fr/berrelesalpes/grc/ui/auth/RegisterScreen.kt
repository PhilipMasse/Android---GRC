package fr.berrelesalpes.grc.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistered: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Créer un compte",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))

            state.errorMessage?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GrcTextField(
                    value = state.prenom,
                    onValueChange = { viewModel.onFieldChange(prenom = it) },
                    label = "Prénom",
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                )
                GrcTextField(
                    value = state.nom,
                    onValueChange = { viewModel.onFieldChange(nom = it) },
                    label = "Nom",
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                )
            }
            Spacer(Modifier.height(12.dp))
            GrcTextField(
                value = state.email,
                onValueChange = { viewModel.onFieldChange(email = it) },
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading,
            )
            Spacer(Modifier.height(12.dp))
            GrcTextField(
                value = state.password,
                onValueChange = { viewModel.onFieldChange(password = it) },
                label = "Mot de passe (8 caractères minimum)",
                isPassword = true,
                enabled = !state.isLoading,
            )

            Spacer(Modifier.height(20.dp))
            Text(text = "Vérification anti-robot", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(text = state.captchaQuestion, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            GrcTextField(
                value = state.captchaReponse,
                onValueChange = { viewModel.onFieldChange(captchaReponse = it) },
                label = "Réponse",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.width(140.dp),
                enabled = !state.isLoading,
            )

            Spacer(Modifier.height(28.dp))
            GrcPrimaryButton(
                text = "Créer mon compte",
                onClick = viewModel::submit,
                isLoading = state.isLoading,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
