package fr.berrelesalpes.grc.ui.demandes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.data.model.DemandeStatuts
import fr.berrelesalpes.grc.ui.common.DateFormatters
import fr.berrelesalpes.grc.ui.common.ErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandeDetailScreen(
    viewModel: DemandeDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.demande?.numeroSuivi ?: "Signalement") },
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
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.demande == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    state.errorMessage?.let { ErrorBanner(it) }
                }
            }
            else -> {
                val demande = state.demande!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = demande.titre ?: "Signalement", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(text = "Statut : ${DemandeStatuts.label(demande.statut)}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Signalé le " + DateFormatters.formatDate(demande.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                    )
                    demande.resolvedAt?.let {
                        Text(
                            text = "Résolu le " + DateFormatters.formatDate(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    if (!demande.description.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF4F6F9), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Text(text = "Description", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(text = demande.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (demande.piecesJointes.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(text = "Photos jointes", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        demande.piecesJointes.forEach { piece ->
                            Text(text = "• " + piece.nomOriginal, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
