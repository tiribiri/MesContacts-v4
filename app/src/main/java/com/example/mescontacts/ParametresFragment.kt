package com.example.mescontacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

/**
 * Écran Paramètres : trois préférences stockées dans les SharedPreferences (pas dans SQLite).
 * Le résumé de chaque préférence affiche la valeur choisie (useSimpleSummaryProvider).
 */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.title_settings)
    }

    companion object {
        const val TAG = "parametres"
    }
}
