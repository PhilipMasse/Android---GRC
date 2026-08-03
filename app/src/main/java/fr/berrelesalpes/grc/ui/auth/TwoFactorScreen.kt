package fr.berrelesalpes.grc.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun TwoFactorScreen(
    viewModel: TwoFactorViewModel,
    onVerified: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.verified) {
        if (state.verified) onVerified()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Vérification supplémentaire",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (viewModel.method == "totp") {
                    "Saisissez le code affiché par votre application d'authentification."
                } else {
                    "Un code vous a été envoyé par email. Il expire dans 5 minutes."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            state.errorMessage?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(16.dp))
            }

            GrcTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = "Code à 6 chiffres",
                keyboardType = KeyboardType.NumberPassword,
                enabled = !state.isLoading,
            )
            Spacer(Modifier.height(24.dp))

            GrcPrimaryButton(
                text = "Valider",
                onClick = viewModel::submit,
                isLoading = state.isLoading,
            )
        }
    }
}
