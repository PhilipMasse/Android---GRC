package fr.berrelesalpes.grc.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onRequiresTwoFactor: (PendingTwoFactor) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loginSucceeded) {
        if (state.loginSucceeded) onLoginSuccess()
    }
    LaunchedEffect(state.pendingTwoFactor) {
        state.pendingTwoFactor?.let(onRequiresTwoFactor)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Mairie de Berre-les-Alpes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mon espace citoyen",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(32.dp))

            state.errorMessage?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(16.dp))
            }

            GrcTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading,
            )
            Spacer(Modifier.height(12.dp))
            GrcTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Mot de passe",
                isPassword = true,
                enabled = !state.isLoading,
            )
            Spacer(Modifier.height(24.dp))

            GrcPrimaryButton(
                text = "Se connecter",
                onClick = viewModel::submit,
                isLoading = state.isLoading,
            )

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Mot de passe oublié ?")
            }
            TextButton(onClick = onNavigateToRegister) {
                Text("Pas encore de compte ? Créer un compte")
            }
        }
    }
}
