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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField
import fr.berrelesalpes.grc.ui.common.SuccessBanner

@Composable
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel,
    onDone: () -> Unit,
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
                text = "Nouveau mot de passe",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Choisissez votre nouveau mot de passe (8 caractères minimum).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))

            state.errorMessage?.let {
                ErrorBanner(it)
                Spacer(Modifier.height(16.dp))
            }

            if (state.successMessage != null) {
                SuccessBanner(state.successMessage!!)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDone) {
                    Text("Se connecter →")
                }
            } else {
                GrcTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Nouveau mot de passe",
                    isPassword = true,
                    enabled = !state.isLoading,
                )
                Spacer(Modifier.height(20.dp))
                GrcPrimaryButton(
                    text = "Valider le nouveau mot de passe",
                    onClick = viewModel::submit,
                    isLoading = state.isLoading,
                )
            }
        }
    }
}
