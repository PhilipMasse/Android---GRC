package fr.berrelesalpes.grc.ui.demandes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.berrelesalpes.grc.data.location.CameraCaptureHelper
import fr.berrelesalpes.grc.ui.common.ErrorBanner
import fr.berrelesalpes.grc.ui.common.GrcPrimaryButton
import fr.berrelesalpes.grc.ui.common.GrcTextField
import fr.berrelesalpes.grc.ui.common.GoogleMapView
import fr.berrelesalpes.grc.ui.common.SuccessBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandeFormScreen(
    viewModel: DemandeFormViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { accorde -> if (accorde) viewModel.localiserAutomatiquement(context) }

    // Demande la géolocalisation dès l'ouverture de l'écran (comme le site
    // web), après vérification/obtention de la permission.
    LaunchedEffect(Unit) {
        val dejaAccorde = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (dejaAccorde) {
            viewModel.localiserAutomatiquement(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> viewModel.onPhotosSelectionnees(uris) }
    val typesPhotosAutorises = arrayOf("image/jpeg", "image/png", "image/webp", "image/gif")

    // Capture directe via l'appareil photo : nécessite un fichier temporaire
    // créé AVANT de lancer l'appareil photo (son Uri content:// est passé en
    // paramètre), d'où la variable d'état conservant la référence entre les
    // deux étapes (préparation puis résultat de la capture).
    var fichierPhotoEnCours by remember { mutableStateOf<Pair<java.io.File, Uri>?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { succes ->
        val prepare = fichierPhotoEnCours
        if (succes && prepare != null) {
            viewModel.onPhotosSelectionnees(listOf(prepare.second))
        }
        fichierPhotoEnCours = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { accorde ->
        if (accorde) {
            val prepare = CameraCaptureHelper.creerFichierPhotoTemporaire(context)
            fichierPhotoEnCours = prepare
            cameraLauncher.launch(prepare.second)
        }
    }
    fun lancerCapturePhoto() {
        val dejaAccorde = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (dejaAccorde) {
            val prepare = CameraCaptureHelper.creerFichierPhotoTemporaire(context)
            fichierPhotoEnCours = prepare
            cameraLauncher.launch(prepare.second)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau signalement") },
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
        if (state.submittedNumero != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                SuccessBanner("Votre signalement ${state.submittedNumero} a bien été envoyé. Vous pouvez suivre son avancement depuis \"Mes signalements\".")
                state.avertissementPhotos?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(it)
                }
                Spacer(Modifier.height(20.dp))
                GrcPrimaryButton(text = "Retour à mes signalements", onClick = onSubmitted)
            }
            return@Scaffold
        }

        // LazyColumn plutôt que Column + verticalScroll : une WebView (la
        // carte) intégrée dans un Column à défilement classique ne s'affiche
        // pas de façon fiable (problème connu de Jetpack Compose), chaque
        // section étant ici un "item" mesuré/composé indépendamment.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            item {
                state.errorMessage?.let {
                    ErrorBanner(it)
                    Spacer(Modifier.height(16.dp))
                }

                GrcTextField(value = state.titre, onValueChange = viewModel::onTitreChange, label = "Objet du signalement *", enabled = !state.isSubmitting)
                Spacer(Modifier.height(12.dp))
                GrcTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = "Description *",
                    enabled = !state.isSubmitting,
                )
                Spacer(Modifier.height(12.dp))

                // --- Catégorie ---
                var menuCategorieOuvert by remember { mutableStateOf(false) }
                val categorieChoisie = state.categories.firstOrNull { it.id == state.categorieId }
                Text(text = "Catégorie", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = menuCategorieOuvert,
                    onExpandedChange = { if (!state.isSubmitting) menuCategorieOuvert = it },
                ) {
                    OutlinedTextField(
                        value = categorieChoisie?.nom ?: "— Sélectionner —",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuCategorieOuvert) },
                    )
                    DropdownMenu(
                        expanded = menuCategorieOuvert,
                        onDismissRequest = { menuCategorieOuvert = false },
                        modifier = Modifier.exposedDropdownSize(),
                    ) {
                        DropdownMenuItem(
                            text = { Text("— Sélectionner —") },
                            onClick = { viewModel.onCategorieChange(null); menuCategorieOuvert = false },
                        )
                        state.categories.forEach { categorie ->
                            DropdownMenuItem(
                                text = { Text(categorie.nom) },
                                onClick = { viewModel.onCategorieChange(categorie.id); menuCategorieOuvert = false },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(text = "Localisation", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                if (state.isLocalisationEnCours) {
                    Row {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Localisation en cours…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                GoogleMapView(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    onPositionSelected = { lat, lng -> viewModel.onPositionChoisie(context, lat, lng) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                state.adresse?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Text(
                    text = "Touchez ou glissez le repère pour ajuster précisément l'emplacement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )

                if (state.demandesProches.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3CD), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ ${state.demandesProches.size} signalement(s) déjà en cours à proximité — vérifiez qu'il ne s'agit pas du même problème :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF664D03),
                        )
                        state.demandesProches.forEach { proche ->
                            Text(
                                text = "• ${proche.titre} — ${proche.statut}, à ${proche.distanceM} m (${proche.date})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF664D03),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(text = "Photos (facultatif)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row {
                    OutlinedButton(onClick = { lancerCapturePhoto() }, enabled = !state.isSubmitting) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Prendre une photo")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { photoPickerLauncher.launch(typesPhotosAutorises) }, enabled = !state.isSubmitting) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Choisir")
                    }
                }
            }

            items(state.photosSelectionnees) { uri ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F6F9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = uri.lastPathSegment ?: "Photo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.retirerPhoto(uri) }, enabled = !state.isSubmitting) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            item {
                Spacer(Modifier.height(24.dp))
                GrcPrimaryButton(
                    text = "Envoyer le signalement",
                    onClick = { viewModel.submit(context.contentResolver, context.cacheDir) },
                    isLoading = state.isSubmitting,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
