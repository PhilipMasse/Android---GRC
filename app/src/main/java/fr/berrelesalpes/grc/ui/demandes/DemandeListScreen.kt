package fr.berrelesalpes.grc.ui.demandes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import fr.berrelesalpes.grc.data.model.DemandeSignalement
import fr.berrelesalpes.grc.data.model.DemandeStatuts
import fr.berrelesalpes.grc.ui.common.DateFormatters
import fr.berrelesalpes.grc.ui.common.ErrorBanner

private fun statutColor(statut: String?): Color = when (statut) {
    "resolu" -> Color(0xFF587526)
    "reouvert" -> Color(0xFFB32D2E)
    "en_cours", "assigne" -> Color(0xFF8A6414)
    "cloture" -> Color(0xFF666666)
    else -> Color(0xFF2D6AB0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandeListScreen(
    viewModel: DemandeListViewModel,
    onBack: () -> Unit,
    onOpenDemande: (Int) -> Unit,
    onNewDemande: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes signalements") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDemande, containerColor = MaterialTheme.colorScheme.tertiary) {
                Icon(Icons.Filled.Add, contentDescription = "Nouveau signalement")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    Box(modifier = Modifier.padding(16.dp)) { ErrorBanner(state.errorMessage!!) }
                }
                state.demandes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Vous n'avez pas encore de signalement. Utilisez le bouton + pour en créer un.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.demandes) { demande ->
                            DemandeCard(demande = demande, onClick = { onOpenDemande(demande.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemandeCard(demande: DemandeSignalement, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F6F9), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = demande.titre ?: "Signalement",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier
                    .background(statutColor(demande.statut), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(text = DemandeStatuts.label(demande.statut), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = demande.numeroSuivi ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            text = "Créé le " + DateFormatters.formatDate(demande.createdAt),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
    }
}
