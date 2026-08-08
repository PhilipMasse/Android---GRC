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
import android.widget.TextView

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
    val onPositionSelectedState = rememberUpdatedState(onPositionSelected)

    AndroidView(
        modifier = modifier.fillMaxWidth().height(260.dp),
        factory = { context ->
            Log.e("LeafletMapView", "AVANT création WebView")
            try {
                val webView = WebView(context)
                Log.e("LeafletMapView", "WebView construite avec succès")

                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
                webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                Log.e("LeafletMapView", "Réglages appliqués")

                webView.webViewClient = object : WebViewClient() {
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
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        Log.e("LeafletMapView", "Console JS : ${message?.message()} (ligne ${message?.lineNumber()} — ${message?.sourceId()})")
                        return true
                    }
                }
                Log.e("LeafletMapView", "Clients assignés")

                webView.addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPositionSelected(lat: Double, lng: Double) {
                            onPositionSelectedState.value(lat, lng)
                        }
                    },
                    "AndroidBridge"
                )
                Log.e("LeafletMapView", "Pont JavaScript ajouté")

                webView.loadUrl("file:///android_asset/map.html")
                Log.e("LeafletMapView", "loadUrl() appelé — retour de la WebView à Compose")
                webView
            } catch (e: Throwable) {
                Log.e("LeafletMapView", "EXCEPTION lors de la création de la WebView", e)
                // En cas d'échec total, on retourne un simple texte plutôt que
                // de laisser planter l'écran, pour que le reste du formulaire
                // reste utilisable.
                TextView(context).apply {
                    text = "Impossible de charger la carte : ${e.message}"
                }
            }
        },
        update = { view ->
            if (view is WebView && latitude != null && longitude != null) {
                view.evaluateJavascript("definirPosition($latitude, $longitude);", null)
            }
        },
    )
}
