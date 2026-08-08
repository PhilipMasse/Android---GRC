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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box

private const val DIAGNOSTIC_TEMPORAIRE = true

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
    Log.e("LeafletMapView", ">>> LeafletMapView composable EXÉCUTÉ (début de fonction) <<<")

    if (DIAGNOSTIC_TEMPORAIRE) {
        // Bloc de diagnostic temporaire : si ce rectangle orange s'affiche,
        // le composant est bien atteint et le problème vient spécifiquement
        // de la WebView. S'il n'apparaît PAS non plus, le problème est en
        // amont (l'appel à LeafletMapView lui-même, ou Compose).
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFFFF6600)),
            contentAlignment = Alignment.Center,
        ) {
            Text("DIAGNOSTIC : ce rectangle doit être visible")
        }
        return
    }

    // rememberUpdatedState évite de recréer la WebView (et donc de recharger
    // la page) à chaque changement de la lambda de callback.
    val onPositionSelectedState = rememberUpdatedState(onPositionSelected)

    AndroidView(
        modifier = modifier.fillMaxWidth().height(260.dp),
        factory = { context ->
            Log.e("LeafletMapView", "Création de la WebView (factory appelée)")
            WebView(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e("LeafletMapView", "Erreur de chargement : ${error?.description} (${request?.url})")
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.e("LeafletMapView", "Page chargée avec succès : $url")
                    }
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.e("LeafletMapView", "Chargement démarré : $url")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        Log.e("LeafletMapView", "Console JS : ${message?.message()} (ligne ${message?.lineNumber()} — ${message?.sourceId()})")
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
                Log.e("LeafletMapView", "loadUrl() appelé")
            }
        },
        update = { view ->
            if (latitude != null && longitude != null) {
                view.evaluateJavascript("definirPosition($latitude, $longitude);", null)
            }
        },
    )
}
