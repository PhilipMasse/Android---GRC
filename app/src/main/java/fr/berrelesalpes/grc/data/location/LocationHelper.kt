package fr.berrelesalpes.grc.data.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Récupère la position actuelle via l'API Android native (LocationManager),
 * plutôt que Google Play Services : évite une dépendance supplémentaire et
 * fonctionne aussi sur les appareils sans services Google.
 *
 * L'appelant est responsable de vérifier que la permission de localisation
 * a été accordée avant d'appeler [obtenirPositionActuelle] — voir l'écran
 * appelant pour la demande de permission (rememberLauncherForActivityResult).
 */
object LocationHelper {

    suspend fun obtenirPositionActuelle(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }

                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    { command -> command.run() }, // Exécution directe : déjà appelé depuis un contexte coroutine IO.
                ) { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            } catch (e: SecurityException) {
                // Permission retirée entre-temps (rare, mais possible) : on
                // échoue proprement plutôt que de laisser planter l'appli.
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }
}
