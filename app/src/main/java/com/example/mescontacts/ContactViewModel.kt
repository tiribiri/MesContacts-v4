package com.example.mescontacts

import android.app.Application
import android.database.SQLException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mescontacts.data.Contact
import com.example.mescontacts.data.ContactDao
import com.example.mescontacts.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel partagé (portée MainActivity, via activityViewModels()).
 * Il expose la liste, lance chaque appel au DAO dans viewModelScope + Dispatchers.IO
 * (jamais sur le thread principal) et recharge la liste après chaque modification.
 */
class ContactViewModel(
    application: Application,
    private val state: SavedStateHandle
) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))

    // Pas de valeur initiale : la liste n'est affichée qu'une fois lue en base (pas de « liste vide » trompeuse)
    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private var chargement: Job? = null

    /** Texte de recherche, conservé en cas de rotation ou de mort du processus. */
    val query: String get() = state[KEY_QUERY] ?: ""

    init {
        rafraichir()
    }

    fun setQuery(q: String) {
        if (q == query) return
        state[KEY_QUERY] = q
        rafraichir()
    }

    /** Recharge la liste depuis la base (filtrée en SQL si une recherche est saisie). */
    fun rafraichir() {
        chargement?.cancel()
        val q = query
        chargement = viewModelScope.launch {
            val liste = withContext(Dispatchers.IO) {
                try {
                    if (q.isBlank()) dao.getAll() else dao.search(q)
                } catch (e: SQLException) {
                    emptyList()
                }
            }
            _contacts.value = liste
        }
    }

    /** Lecture d'un contact par son _id (null si introuvable). */
    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try {
            dao.getById(id)
        } catch (e: SQLException) {
            null
        }
    }

    /** Résultat d'un enregistrement, pour afficher le bon message dans l'interface. */
    sealed class Resultat {
        data class Insere(val id: Long) : Resultat()
        object MisAJour : Resultat()
        object Introuvable : Resultat()
        object Echec : Resultat()
    }

    /** id == 0 → insert ; sinon update. Supprime l'ancienne photo si elle a été remplacée. */
    suspend fun enregistrer(contact: Contact): Resultat {
        val resultat = withContext(Dispatchers.IO) {
            try {
                if (contact.id == NOUVEAU_CONTACT) {
                    val id = dao.insert(contact)
                    if (id == -1L) Resultat.Echec else Resultat.Insere(id)
                } else {
                    val ancien = dao.getById(contact.id)
                    val lignes = dao.update(contact)
                    if (lignes == 0) {
                        Resultat.Introuvable
                    } else {
                        val anciennePhoto = ancien?.photoUri
                        if (anciennePhoto != null && anciennePhoto != contact.photoUri) {
                            ImageUtils.deleteQuietly(anciennePhoto)
                        }
                        Resultat.MisAJour
                    }
                }
            } catch (e: SQLException) {
                Resultat.Echec
            }
        }
        rafraichir()
        return resultat
    }

    /** Supprime la ligne (et le fichier photo). Retourne true si une ligne a été supprimée. */
    suspend fun supprimer(id: Long): Boolean {
        val ok = withContext(Dispatchers.IO) {
            try {
                val photo = dao.getById(id)?.photoUri
                val lignes = dao.delete(id)
                if (lignes > 0) ImageUtils.deleteQuietly(photo)
                lignes > 0
            } catch (e: SQLException) {
                false
            }
        }
        rafraichir()
        return ok
    }

    companion object {
        private const val KEY_QUERY = "query"
        /** Un contact non enregistré a l'id 0. */
        const val NOUVEAU_CONTACT = 0L
    }
}
