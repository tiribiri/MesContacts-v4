package com.example.mescontacts

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

/** Valeurs déjà converties, prêtes à être appliquées aux vues. */
data class PreferencesAffichage(
    val tailleSp: Float,
    val couleur: Int,
    val appelDepuisListe: Boolean
)

/**
 * Lecture centralisée des préférences (SharedPreferences par défaut).
 * Les fragments et l'adapter reçoivent des valeurs converties : taille en sp, couleur en Int, booléen.
 */
class PreferencesManager(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun lire(): PreferencesAffichage {
        val taille = prefs.getString(KEY_TAILLE, DEFAUT_TAILLE)?.toFloatOrNull() ?: DEFAUT_TAILLE.toFloat()
        val couleur = try {
            Color.parseColor(prefs.getString(KEY_COULEUR, DEFAUT_COULEUR) ?: DEFAUT_COULEUR)
        } catch (e: IllegalArgumentException) {
            Color.parseColor(DEFAUT_COULEUR)
        }
        val appel = prefs.getBoolean(KEY_APPEL, false)
        return PreferencesAffichage(taille, couleur, appel)
    }

    companion object {
        const val KEY_TAILLE = "taille_police"
        const val KEY_COULEUR = "couleur_preferee"
        const val KEY_APPEL = "appel_depuis_liste"
        const val DEFAUT_TAILLE = "16"
        const val DEFAUT_COULEUR = "#000000"
    }
}
