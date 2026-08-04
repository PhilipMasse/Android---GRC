package fr.berrelesalpes.grc.ui.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * L'API renvoie les dates au format MySQL ("2026-08-04 22:09:13", heure
 * locale du site — voir current_time('mysql') côté plugin WordPress).
 * Ces fonctions les convertissent vers un affichage français lisible.
 */
object DateFormatters {

    private val ENTREE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val SORTIE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val SORTIE_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")

    /** Ex : "2026-08-04 22:09:13" → "04/08/2026". Retourne "—" si non fourni ou illisible. */
    fun formatDate(brut: String?): String {
        if (brut.isNullOrBlank()) return "—"
        return try {
            LocalDateTime.parse(brut, ENTREE).format(SORTIE_DATE)
        } catch (e: DateTimeParseException) {
            brut // Valeur imprévue : on l'affiche telle quelle plutôt que de la masquer.
        }
    }

    /** Ex : "2026-08-04 22:09:13" → "04/08/2026 à 22:09". Retourne "—" si non fourni ou illisible. */
    fun formatDateTime(brut: String?): String {
        if (brut.isNullOrBlank()) return "—"
        return try {
            LocalDateTime.parse(brut, ENTREE).format(SORTIE_DATE_HEURE)
        } catch (e: DateTimeParseException) {
            brut
        }
    }
}
