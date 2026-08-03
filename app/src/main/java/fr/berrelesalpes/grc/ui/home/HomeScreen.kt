package fr.berrelesalpes.grc.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.ui.common.ErrorBanner

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLoggedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon espace citoyen") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            when {
                state.isLoading -> {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    ErrorBanner(state.errorMessage!!)
                    Spacer(Modifier.weight(1f))
                }
                state.citoyen != null -> {
                    val c = state.citoyen!!
                    Text(
                        text = "Bonjour ${c.prenom ?: ""} ${c.nom ?: ""}".trim(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = c.email ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (c.twoFactorMethod != null) {
                            "🔒 Double authentification activée (${c.twoFactorMethod})"
                        } else {
                            "⚠️ Double authentification non activée"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (c.twoFactorMethod != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = "Les modules Signalements, Démarches et Rendez-vous seront ajoutés dans une prochaine version.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }

            OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                Text("Se déconnecter")
            }
        }
    }
}
