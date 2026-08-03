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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField
import fr.berrelesalpes.grc.ui.common.SuccessBanner

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Mot de passe oublié",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Indiquez votre email : si un compte existe, un lien de réinitialisation vous sera envoyé.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))

            state.errorMessage?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(16.dp))
            }
            state.successMessage?.let {
                SuccessBanner(it)
                Spacer(Modifier.height(16.dp))
            }

            GrcTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading && state.successMessage == null,
            )
            Spacer(Modifier.height(20.dp))

            if (state.successMessage == null) {
                GrcPrimaryButton(
                    text = "Envoyer le lien",
                    onClick = viewModel::submit,
                    isLoading = state.isLoading,
                )
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBackToLogin) {
                Text("← Retour à la connexion")
            }
        }
    }
}
