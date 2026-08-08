package fr.berrelesalpes.grc.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Categorie(
    val id: Int,
    val nom: String,
    @Json(name = "parent_id") val parentId: Int? = null,
    @Json(name = "service_id") val serviceId: Int? = null,
)

/**
 * Forme unique retournée à la fois par la liste (GET /mes-demandes) et le
 * détail — le plugin WordPress renvoie déjà toutes les informations utiles
 * dans la liste (voir GRC_REST_Demandes::format_demande_public), donc
 * l'application n'a pas besoin d'un second appel réseau pour afficher le
 * détail d'un signalement.
 */
@JsonClass(generateAdapter = true)
data class DemandeSignalement(
    val id: Int,
    @Json(name = "numero_suivi") val numeroSuivi: String?,
    val titre: String?,
    val description: String?,
    val statut: String?,
    val priorite: String? = null,
    @Json(name = "categorie_id") val categorieId: Int? = null,
    @Json(name = "service_id") val serviceId: Int? = null,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "resolved_at") val resolvedAt: String? = null,
    @Json(name = "peut_etre_note") val peutEtreNote: Boolean = false,
    @Json(name = "pieces_jointes") val piecesJointes: List<PieceJointe> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SubmitDemandeRequest(
    val titre: String,
    val description: String,
    @Json(name = "categorie_id") val categorieId: Int?,
    val latitude: Double?,
    val longitude: Double?,
    @Json(name = "adresse_lieu") val adresseLieu: String?,
)

@JsonClass(generateAdapter = true)
data class SubmitDemandeResponse(
    val id: Int? = null,
    @Json(name = "numero_suivi") val numeroSuivi: String? = null,
    val statut: String? = null,
)

/** Signalement existant à proximité, pour avertir d'un doublon potentiel avant envoi. */
@JsonClass(generateAdapter = true)
data class DemandeProche(
    val titre: String,
    val statut: String,
    @Json(name = "distance_m") val distanceM: Int,
    val date: String,
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodeResponse(
    val adresse: String,
    val brut: String? = null,
)

/** Statuts possibles d'un signalement (voir class-grc-admin-demandes.php côté plugin). */
object DemandeStatuts {
    val labels = mapOf(
        "nouveau" to "Nouveau",
        "en_cours" to "En cours",
        "assigne" to "Assigné",
        "resolu" to "Résolu",
        "cloture" to "Clôturé",
        "reouvert" to "Réouvert",
    )

    fun label(statut: String?): String = statut?.let { labels[it] ?: it } ?: "Statut inconnu"
}
