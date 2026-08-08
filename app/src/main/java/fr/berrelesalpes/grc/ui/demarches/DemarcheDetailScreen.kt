package fr.berrelesalpes.grc.ui.demarches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.data.model.DemarcheDetail
import fr.berrelesalpes.grc.data.model.DemarcheMessage
import fr.berrelesalpes.grc.data.model.DemarcheStatuts
import fr.berrelesalpes.grc.ui.common.DateFormatters
import fr.berrelesalpes.grc.ui.common.ErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemarcheDetailScreen(
    viewModel: DemarcheDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> viewModel.onFichiersSelectionnes(uris) }
    val typesDocumentsAutorises = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.dossier?.numeroDossier ?: "Démarche") },
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
            state.isLoading && state.dossier == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.dossier == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    state.errorMessage?.let { ErrorBanner(it) }
                }
            }
            else -> {
                val dossier = state.dossier!!
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        DossierRecap(dossier)

                        Spacer(Modifier.height(20.dp))
                        Text(text = "Échanges", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))

                        if (dossier.messages.isEmpty()) {
                            Text(
                                text = "Aucun message pour le moment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                            )
                        } else {
                            dossier.messages.forEach { message ->
                                MessageBubble(message)
                                Spacer(Modifier.height(10.dp))
                            }
                        }

                        state.errorMessage?.let {
                            Spacer(Modifier.height(12.dp))
                            ErrorBanner(it)
                        }
                    }

                    // Fichiers en attente d'envoi avec le prochain message.
                    if (state.fichiersMessage.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            state.fichiersMessage.forEach { uri ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEEF4FA), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = uri.lastPathSegment ?: "Document",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.retirerFichier(uri) }, enabled = !state.isSendingMessage) {
                                        Icon(Icons.Filled.Close, contentDescription = "Retirer", modifier = Modifier.width(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                    // Zone de saisie d'un nouveau message, fixée en bas de l'écran.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { filePickerLauncher.launch(typesDocumentsAutorises) },
                            enabled = !state.isSendingMessage,
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Joindre un document", tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = state.nouveauMessage,
                            onValueChange = viewModel::onMessageChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Écrire un message…") },
                            enabled = !state.isSendingMessage,
                            maxLines = 4,
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.sendMessage(context.contentResolver, context.cacheDir) },
                            enabled = !state.isSendingMessage && (state.nouveauMessage.isNotBlank() || state.fichiersMessage.isNotEmpty()),
                        ) {
                            if (state.isSendingMessage) {
                                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Envoyer",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DossierRecap(dossier: DemarcheDetail) {
    Text(text = dossier.typeNom ?: dossier.typeDemarche ?: "Démarche", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    Text(text = "Statut : ${DemarcheStatuts.label(dossier.statut)}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "Déposée le " + fr.berrelesalpes.grc.ui.common.DateFormatters.formatDate(dossier.createdAt), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    Spacer(Modifier.height(12.dp))

    if (dossier.champs.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4F6F9), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            dossier.champs.forEach { champ ->
                val valeur = dossier.donnees[champ.key]?.toString()
                if (!valeur.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = champ.label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(text = valeur, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: DemarcheMessage) {
    val estAgent = message.auteurType == "agent"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (estAgent) Color(0xFFEAF1E4) else Color(0xFFEEF4FA),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = if (estAgent) "Agent municipal" else "Vous",
            style = MaterialTheme.typography.labelLarge,
            color = if (estAgent) Color(0xFF587526) else Color(0xFF2D6AB0),
        )
        Spacer(Modifier.height(4.dp))
        Text(text = message.contenu, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = fr.berrelesalpes.grc.ui.common.DateFormatters.formatDateTime(message.createdAt),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
    }
}
