package fr.berrelesalpes.grc.ui.demarches

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
import fr.berrelesalpes.grc.data.model.DemarcheResume
import fr.berrelesalpes.grc.data.model.DemarcheStatuts
import fr.berrelesalpes.grc.ui.common.ErrorBanner

private fun statutColor(statut: String?): Color = when (statut) {
    "valide" -> Color(0xFF587526)
    "rejete" -> Color(0xFFB32D2E)
    "complement_requis" -> Color(0xFF8A6414)
    else -> Color(0xFF2D6AB0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemarcheListScreen(
    viewModel: DemarcheListViewModel,
    onBack: () -> Unit,
    onOpenDemarche: (Int) -> Unit,
    onNewDemarche: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes démarches") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
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
            FloatingActionButton(onClick = onNewDemarche, containerColor = MaterialTheme.colorScheme.tertiary) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle démarche")
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
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBanner(state.errorMessage!!)
                    }
                }
                state.demarches.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Vous n'avez pas encore de démarche en cours. Utilisez le bouton + pour en créer une.",
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
                        items(state.demarches) { demarche ->
                            DemarcheCard(demarche = demarche, onClick = { onOpenDemarche(demarche.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemarcheCard(demarche: DemarcheResume, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F6F9), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = demarche.typeNom ?: demarche.typeDemarche ?: "Démarche",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier
                    .background(statutColor(demarche.statut), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = DemarcheStatuts.label(demarche.statut),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = demarche.numeroDossier ?: "Numéro en attente d'attribution", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}
