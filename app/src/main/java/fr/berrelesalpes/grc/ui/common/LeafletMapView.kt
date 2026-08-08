package fr.berrelesalpes.grc.ui.common

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Carte Leaflet/OpenStreetMap affichée dans une WebView, avec un repère
 * déplaçable (glisser ou toucher la carte). Même bibliothèque et même CDN
 * que le site web (voir map.html dans les assets), pour ne pas ajouter de
 * dépendance de cartographie native ni de clé API.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    latitude: Double?,
    longitude: Double?,
    onPositionSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberUpdatedState évite de recréer la WebView (et donc de recharger
    // la page) à chaque changement de la lambda de callback.
    val onPositionSelectedState = rememberUpdatedState(onPositionSelected)

    AndroidView(
        modifier = modifier.fillMaxWidth().height(260.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPositionSelected(lat: Double, lng: Double) {
                            onPositionSelectedState.value(lat, lng)
                        }
                    },
                    "AndroidBridge"
                )
                loadUrl("file:///android_asset/map.html")
            }
        },
        update = { view ->
            if (latitude != null && longitude != null) {
                view.evaluateJavascript("definirPosition($latitude, $longitude);", null)
            }
        },
    )
}
