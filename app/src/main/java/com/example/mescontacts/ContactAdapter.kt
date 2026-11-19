package com.example.mescontacts

import android.graphics.Bitmap
import android.util.LruCache
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mescontacts.data.Contact
import com.example.mescontacts.databinding.ItemContactBinding

/**
 * Adapter de la RecyclerView. Il reçoit les préférences déjà converties (taille en sp,
 * couleur en Int, booléen) et transmet toujours le Contact (donc son _id), jamais une position.
 */
class ContactAdapter(
    private val onClick: (Contact) -> Unit,
    private val onAppeler: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DIFF) {

    private val cache = LruCache<String, Bitmap>(40)

    private var prefs = PreferencesAffichage(16f, android.graphics.Color.BLACK, false)

    init {
        setHasStableIds(true)
    }

    /** Applique de nouvelles préférences : les lignes visibles sont redessinées immédiatement. */
    fun appliquerPreferences(nouvelles: PreferencesAffichage) {
        if (nouvelles == prefs) return
        prefs = nouvelles
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var contact: Contact? = null

        init {
            binding.root.setOnClickListener { contact?.let(onClick) }
            // Le bouton consomme son propre clic : la fiche détaillée ne s'ouvre pas
            binding.btnCallRow.setOnClickListener { contact?.let(onAppeler) }
        }

        fun bind(c: Contact) {
            contact = c
            binding.txtName.text = c.nomComplet
            binding.txtPhone.text = c.telephone

            // Préférences : taille (nom, prénom, téléphone) et couleur (nom, prénom)
            binding.txtName.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp)
            binding.txtPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp)
            binding.txtName.setTextColor(prefs.couleur)
            binding.btnCallRow.visibility = if (prefs.appelDepuisListe) View.VISIBLE else View.GONE

            binding.imgPin.visibility = if (c.aUnePosition) View.VISIBLE else View.GONE

            val bitmap = c.photoUri?.let { path ->
                cache.get(path) ?: ImageUtils.decodeScaled(path, 160)?.also { cache.put(path, it) }
            }
            if (bitmap != null) {
                binding.imgPhoto.setImageBitmap(bitmap)
            } else {
                binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(old: Contact, new: Contact) = old.id == new.id
            override fun areContentsTheSame(old: Contact, new: Contact) = old == new
        }
    }
}
