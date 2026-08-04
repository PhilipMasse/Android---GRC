package fr.berrelesalpes.grc.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Un champ de formulaire déclaré par un type de démarche. Le champ [type]
 * reprend exactement les valeurs gérées par l'écran d'administration du
 * plugin (assets/admin.js — TYPE_LABELS) : "text", "textarea", "email",
 * "number", "date", "phone", "file".
 */
@JsonClass(generateAdapter = true)
data class ChampDemarche(
    val key: String,
    val label: String,
    val type: String, // "text" | "textarea" | "email" | "number" | "date" | "phone" | "file"
    val requis: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class DemarcheType(
    val id: Int,
    val nom: String,
    val slug: String,
    val description: String?,
    val champs: List<ChampDemarche> = emptyList(),
)

/** Élément de la liste "Mes démarches" — version résumée, sans les champs/données. */
@JsonClass(generateAdapter = true)
data class DemarcheResume(
    val id: Int,
    @Json(name = "numero_dossier") val numeroDossier: String?,
    @Json(name = "type_demarche") val typeDemarche: String?,
    @Json(name = "type_nom") val typeNom: String?,
    val statut: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
)

@JsonClass(generateAdapter = true)
data class PieceJointe(
    val id: Int,
    @Json(name = "nom_original") val nomOriginal: String,
    @Json(name = "mime_type") val mimeType: String?,
    @Json(name = "download_url") val downloadUrl: String,
)

@JsonClass(generateAdapter = true)
data class DemarcheMessage(
    val id: Int,
    @Json(name = "auteur_type") val auteurType: String, // "agent" | "citoyen"
    val contenu: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "pieces_jointes") val piecesJointes: List<PieceJointe> = emptyList(),
)

/** Détail complet d'un dossier de démarche, tel que retourné par GET /demarches/{id}. */
@JsonClass(generateAdapter = true)
data class DemarcheDetail(
    val id: Int,
    @Json(name = "numero_dossier") val numeroDossier: String?,
    @Json(name = "type_demarche") val typeDemarche: String?,
    @Json(name = "type_nom") val typeNom: String?,
    val statut: String?,
    // Map clé de champ -> valeur saisie par le citoyen (types dynamiques : String, Double, null...).
    val donnees: Map<String, Any?> = emptyMap(),
    val champs: List<ChampDemarche> = emptyList(),
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "pieces_jointes") val piecesJointes: List<PieceJointe> = emptyList(),
    val messages: List<DemarcheMessage> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SubmitDemarcheRequest(
    @Json(name = "type_slug") val typeSlug: String,
    val donnees: Map<String, String>,
)

@JsonClass(generateAdapter = true)
data class SubmitDemarcheResponse(
    val id: Int? = null,
    @Json(name = "numero_dossier") val numeroDossier: String? = null,
)

@JsonClass(generateAdapter = true)
data class AddMessageResponse(
    val success: Boolean,
    val id: Int,
    @Json(name = "pieces_jointes") val piecesJointes: List<PieceJointe> = emptyList(),
)

/** Statuts possibles d'un dossier, avec leur libellé français (voir class-grc-admin-demarches.php). */
object DemarcheStatuts {
    val labels = mapOf(
        "en_attente" to "En attente",
        "en_cours" to "En cours",
        "valide" to "Validé",
        "rejete" to "Rejeté",
        "complement_requis" to "Complément requis",
    )

    fun label(statut: String?): String = statut?.let { labels[it] ?: it } ?: "Statut inconnu"
}
