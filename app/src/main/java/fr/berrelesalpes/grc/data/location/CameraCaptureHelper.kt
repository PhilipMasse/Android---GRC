package fr.berrelesalpes.grc.data.location

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Crée un fichier temporaire dans le cache de l'application et retourne son
 * Uri "content://" (via FileProvider) — c'est ce type d'Uri, et non un
 * chemin de fichier direct, que l'application Appareil photo exige depuis
 * Android 7 pour y écrire la photo capturée.
 */
object CameraCaptureHelper {

    fun creerFichierPhotoTemporaire(context: Context): Pair<File, Uri> {
        val dossier = File(context.cacheDir, "photos").apply { mkdirs() }
        val fichier = File(dossier, "signalement_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
        return fichier to uri
    }
}
