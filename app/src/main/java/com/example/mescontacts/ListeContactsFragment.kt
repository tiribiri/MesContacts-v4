package com.example.mescontacts

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mescontacts.data.Contact
import com.example.mescontacts.databinding.FragmentListeContactsBinding


class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter
    private lateinit var prefsManager: PreferencesManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsManager = PreferencesManager(requireContext())

        adapter = ContactAdapter(
            onClick = { contact -> (requireActivity() as MainActivity).ouvrirDetail(contact.id) },
            onAppeler = { contact -> appeler(contact) }
        )
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        if (binding.editSearch.text.toString() != viewModel.query) {
            binding.editSearch.setText(viewModel.query)
        }
        // Filtrage au fil de la saisie (requête SQL paramétrée dans le DAO)
        binding.editSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewModel.contacts.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            afficherEtatVide(liste.isEmpty())
        }

        // « + » : DetailContactFragment en mode création (id = 0)
        binding.fabAdd.setOnClickListener {
            (requireActivity() as MainActivity).ouvrirDetail(ContactViewModel.NOUVEAU_CONTACT)
        }

        // Menu « Paramètres » dans la barre d'application, visible seulement sur la liste
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_settings) {
                    (requireActivity() as MainActivity).ouvrirParametres()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.title_main)
        // Préférences relues à chaque retour sur la liste : appliquées sans redémarrer
        adapter.appliquerPreferences(prefsManager.lire())
        // La liste est rechargée depuis la base à chaque affichage
        viewModel.rafraichir()
    }

    /** ACTION_DIAL : ouvre le composeur, aucune permission nécessaire (contrairement à ACTION_CALL). */
    private fun appeler(contact: Contact) {
        val numero = contact.telephone.trim()
        if (numero.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_phone_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.msg_no_dialer, Toast.LENGTH_SHORT).show()
        }
    }

    private fun afficherEtatVide(vide: Boolean) {
        binding.txtEmpty.visibility = if (vide) View.VISIBLE else View.GONE
        binding.recyclerContacts.visibility = if (vide) View.GONE else View.VISIBLE
        if (vide) {
            binding.txtEmpty.text = if (viewModel.query.isBlank()) {
                getString(R.string.empty_list)
            } else {
                getString(R.string.empty_search, viewModel.query.trim())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerContacts.adapter = null
        _binding = null
    }

    companion object {
        const val TAG = "liste"
    }
}
