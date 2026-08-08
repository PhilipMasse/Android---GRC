package fr.berrelesalpes.grc.ui.demarches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.data.model.ChampDemarche
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField
import fr.berrelesalpes.grc.ui.common.SuccessBanner
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemarcheFormScreen(
    viewModel: DemarcheFormViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Restreint aux formats acceptés par le serveur pour les démarches
    // (PDF et Word .docx uniquement — voir GRC_File_Scanner::ALLOWED_DOCUMENT_MIME
    // côté plugin WordPress), à l'identique du sélecteur du site web.
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
                title = { Text(state.type?.nom ?: "Nouvelle démarche") },
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
            state.isLoadingType -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.submittedNumero != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    SuccessBanner("Votre dossier ${state.submittedNumero} a bien été envoyé. Vous pouvez suivre son avancement depuis \"Mes démarches\".")
                    state.avertissementFichiers?.let {
                        Spacer(Modifier.height(12.dp))
                        ErrorBanner(it)
                    }
                    Spacer(Modifier.height(20.dp))
                    GrcPrimaryButton(text = "Retour à mes démarches", onClick = onSubmitted)
                }
            }
            state.type != null -> {
                val type = state.type!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!type.description.isNullOrBlank()) {
                        Text(text = type.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                    }

                    state.errorMessage?.let {
                        ErrorBanner(it)
                        Spacer(Modifier.height(16.dp))
                    }

                    type.champs.forEach { champ ->
                        DemarcheChampField(
                            champ = champ,
                            value = state.valeurs[champ.key] ?: "",
                            onValueChange = { viewModel.onValueChange(champ.key, it) },
                            enabled = !state.isSubmitting,
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // Sélecteur de documents commun à tous les champs "fichier" du
                    // formulaire : le citoyen peut choisir plusieurs documents en une
                    // fois (PDF, Word, images...), envoyés au dossier une fois créé.
                    Text(text = "Documents à joindre", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(typesDocumentsAutorises) },
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Choisir un ou plusieurs documents")
                    }
                    if (state.fichiersSelectionnes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        state.fichiersSelectionnes.forEach { uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF4F6F9), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = uri.lastPathSegment ?: "Document",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.retirerFichier(uri) }, enabled = !state.isSubmitting) {
                                    Icon(Icons.Filled.Close, contentDescription = "Retirer")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    GrcPrimaryButton(
                        text = "Envoyer le dossier",
                        onClick = { viewModel.submit(context.contentResolver, context.cacheDir) },
                        isLoading = state.isSubmitting,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    state.errorMessage?.let { ErrorBanner(it) }
                }
            }
        }
    }
}

private val DATE_STOCKAGE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val DATE_AFFICHAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemarcheChampField(
    champ: ChampDemarche,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    if (champ.type == "file") {
        // Les pièces jointes se joignent depuis la liste de documents en bas
        // du formulaire (un seul sélecteur pour tous les champs "fichier"),
        // voir DemarcheFormScreen — ce champ ne fait qu'indiquer sa présence.
        Column {
            Text(
                text = champ.label + if (champ.requis) " *" else "",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "Joignez ce document (PDF ou Word) depuis la section \"Documents à joindre\" ci-dessous.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
        return
    }

    val suffixeObligatoire = if (champ.requis) " *" else ""

    if (champ.type == "date") {
        var afficherDialogue by remember { mutableStateOf(false) }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                java.time.LocalDate.parse(value, DATE_STOCKAGE)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        )

        val texteAffiche = runCatching {
            java.time.LocalDate.parse(value, DATE_STOCKAGE).format(DATE_AFFICHAGE)
        }.getOrDefault("")

        GrcTextField(
            value = texteAffiche,
            onValueChange = {}, // Lecture seule : la saisie passe uniquement par le sélecteur.
            label = champ.label + suffixeObligatoire + " (JJ/MM/AAAA)",
            enabled = false,
            modifier = Modifier.clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { afficherDialogue = true },
        )

        if (afficherDialogue) {
            DatePickerDialog(
                onDismissRequest = { afficherDialogue = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = dateState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onValueChange(date.format(DATE_STOCKAGE))
                        }
                        afficherDialogue = false
                    }) { Text("Valider") }
                },
                dismissButton = {
                    TextButton(onClick = { afficherDialogue = false }) { Text("Annuler") }
                },
            ) {
                DatePicker(state = dateState)
            }
        }
        return
    }

    val keyboardType = when (champ.type) {
        "email" -> KeyboardType.Email
        "number" -> KeyboardType.Number
        "phone" -> KeyboardType.Phone
        else -> KeyboardType.Text
    }

    GrcTextField(
        value = value,
        onValueChange = onValueChange,
        label = champ.label + suffixeObligatoire,
        keyboardType = keyboardType,
        enabled = enabled,
    )
}
