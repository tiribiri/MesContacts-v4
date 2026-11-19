package com.example.mescontacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.example.mescontacts.databinding.ActivityMainBinding

/**
 * Activité unique : barre d'application + FragmentContainerView.
 * Centralise la navigation (replace + addToBackStack) vers la Liste, le Détail, la Carte et les Paramètres.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Valeurs par défaut des préférences au premier lancement (sans écraser les choix existants)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Flèche Retour dans la barre dès qu'un écran est empilé
        supportFragmentManager.addOnBackStackChangedListener { majFlecheRetour() }
        majFlecheRetour()

        // Après une rotation, le FragmentManager recrée lui-même les fragments : pas de doublon
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainer, ListeContactsFragment(), ListeContactsFragment.TAG)
            }
        }
    }

    private fun majFlecheRetour() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    /** La flèche de la barre se comporte comme le bouton Retour (le Détail l'intercepte pour Annuler). */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    fun ouvrirDetail(contactId: Long) =
        naviguer(DetailContactFragment.newInstance(contactId), DetailContactFragment.TAG)

    fun ouvrirCarte(contactId: Long, nom: String, latitude: Double?, longitude: Double?) =
        naviguer(CarteContactFragment.newInstance(contactId, nom, latitude, longitude), CarteContactFragment.TAG)

    fun ouvrirParametres() = naviguer(ParametresFragment(), ParametresFragment.TAG)

    fun retour() {
        if (!supportFragmentManager.isStateSaved) supportFragmentManager.popBackStack()
    }

    private fun naviguer(fragment: Fragment, tag: String) {
        if (supportFragmentManager.isStateSaved) return
        // Évite d'empiler deux fois le même écran (double clic rapide)
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.tag == tag) return
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            replace(R.id.fragmentContainer, fragment, tag)
            addToBackStack(tag)
        }
    }
}
