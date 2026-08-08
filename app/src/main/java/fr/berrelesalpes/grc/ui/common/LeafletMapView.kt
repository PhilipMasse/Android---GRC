package fr.berrelesalpes.grc.ui.common

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
                // Corrige un problème connu et fréquent : une WebView intégrée
                // dans une interface Compose (via AndroidView) reste parfois
                // entièrement blanche à cause d'un conflit avec l'accélération
                // matérielle par défaut. Le rendu logiciel évite ce problème.
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                // Autorise le chargement de ressources distantes (CDN Leaflet,
                // tuiles OpenStreetMap) depuis une page locale (file://), et
                // désactive le cache pour toujours charger la dernière version
                // pendant la mise au point.
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e("LeafletMapView", "Erreur de chargement : ${error?.description} (${request?.url})")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        Log.d("LeafletMapView", "Console JS : ${message?.message()} (ligne ${message?.lineNumber()} — ${message?.sourceId()})")
                        return true
                    }
                }

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
