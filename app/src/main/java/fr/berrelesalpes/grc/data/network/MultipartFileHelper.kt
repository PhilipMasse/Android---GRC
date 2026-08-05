package fr.berrelesalpes.grc.data.network

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Convertit les fichiers choisis par le citoyen (Uri du système, obtenus via
 * un sélecteur de documents) en parties multipart Retrofit. La lecture se
 * fait via le ContentResolver — nécessaire car les Uri de type "content://"
 * ne correspondent pas forcément à un chemin de fichier direct accessible.
 */
object MultipartFileHelper {

    /** Nom du champ attendu par l'API pour plusieurs fichiers dans une même requête. */
    const val CHAMP_FICHIERS = "files[]"

    fun toMultipartParts(resolver: ContentResolver, uris: List<Uri>, cacheDir: File): List<MultipartBody.Part> {
        return uris.mapNotNull { uri -> toMultipartPart(resolver, uri, cacheDir) }
    }

    private fun toMultipartPart(resolver: ContentResolver, uri: Uri, cacheDir: File): MultipartBody.Part? {
        val nomFichier = queryDisplayName(resolver, uri) ?: "document"
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        // Copie temporaire dans le cache de l'application : simplifie la
        // construction du RequestBody et évite de garder le flux ouvert plus
        // longtemps que nécessaire. Le fichier temporaire est nommé de façon
        // unique pour éviter toute collision entre envois simultanés.
        val fichierTemp = File(cacheDir, "upload_${System.currentTimeMillis()}_$nomFichier")
        return try {
            resolver.openInputStream(uri)?.use { input ->
                fichierTemp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val requestBody = fichierTemp.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(CHAMP_FICHIERS, nomFichier, requestBody)
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun textPart(nom: String, valeur: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(nom, null, valeur.toRequestBody("text/plain".toMediaTypeOrNull()))
    }
}
