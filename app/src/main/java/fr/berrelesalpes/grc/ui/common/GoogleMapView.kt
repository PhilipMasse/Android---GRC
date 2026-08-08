package fr.berrelesalpes.grc.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val POSITION_PAR_DEFAUT = LatLng(43.7102, 7.2620) // Nice — recentré dès que la position réelle est connue.

/**
 * Carte Google Maps avec un repère déplaçable (glisser le marqueur ou
 * toucher la carte pour le repositionner), utilisée pour préciser
 * l'emplacement d'un signalement.
 */
@Composable
fun GoogleMapView(
    latitude: Double?,
    longitude: Double?,
    onPositionSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onPositionSelectedState = rememberUpdatedState(onPositionSelected)

    val positionInitiale = remember {
        if (latitude != null && longitude != null) LatLng(latitude, longitude) else POSITION_PAR_DEFAUT
    }
    val markerState = remember { MarkerState(position = positionInitiale) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(positionInitiale, if (latitude != null) 18f else 12f)
    }

    // Recentre la carte et le marqueur si la position est mise à jour depuis
    // l'extérieur (ex : résultat de la géolocalisation automatique), sans
    // écraser une position que le citoyen viendrait d'ajuster lui-même.
    var derniereLatExterne by remember { mutableStateOf(latitude) }
    var derniereLngExterne by remember { mutableStateOf(longitude) }
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null &&
            (latitude != derniereLatExterne || longitude != derniereLngExterne)
        ) {
            val nouvellePosition = LatLng(latitude, longitude)
            markerState.position = nouvellePosition
            cameraPositionState.position = CameraPosition.fromLatLngZoom(nouvellePosition, 18f)
            derniereLatExterne = latitude
            derniereLngExterne = longitude
        }
    }

    // Le glisser-déposer du marqueur met à jour markerState.position en
    // continu pendant le geste ; on ne notifie l'extérieur qu'une fois le
    // geste terminé (dragState == END), pour éviter des dizaines d'appels
    // réseau (géocodage, doublons) pendant le déplacement.
    LaunchedEffect(markerState.dragState) {
        if (markerState.dragState == DragState.END) {
            onPositionSelectedState.value(markerState.position.latitude, markerState.position.longitude)
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxWidth().height(260.dp),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false),
        onMapClick = { latLng ->
            markerState.position = latLng
            onPositionSelectedState.value(latLng.latitude, latLng.longitude)
        },
    ) {
        Marker(
            state = markerState,
            draggable = true,
        )
    }
}
