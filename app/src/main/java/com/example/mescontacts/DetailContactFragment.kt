package com.example.mescontacts

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.mescontacts.data.Contact
import com.example.mescontacts.databinding.FragmentDetailContactBinding
import kotlinx.coroutines.launch
import java.io.File

/**
 * Écran Détail : création (id = 0), consultation, modification et suppression d'un contact,
 * actions Appeler, E-mail, Galerie, Caméra et Voir sur la carte.
 * Reçoit uniquement l'_id du contact dans son Bundle ; le contact est relu en base (hors thread principal).
 */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId = ContactViewModel.NOUVEAU_CONTACT
    private val modeCreation get() = contactId == ContactViewModel.NOUVEAU_CONTACT

    /**
     * Vrai une fois les champs remplis depuis la base. Conservé dans l'instance (retour de la Carte)
     * et dans le Bundle (rotation) : une saisie en cours n'est jamais écrasée.
     */
    private var champsInitialises = false

    /** Une écriture en base est en cours (évite un double enregistrement). */
    private var operationEnCours = false

    /** Photo affichée (non enregistrée tant qu'on n'a pas appuyé sur « Enregistrer »). */
    private var currentPhotoPath: String? = null

    /** Fichier temporaire donné à l'application caméra. */
    private var cameraTempFile: File? = null

    /** Photos créées pendant l'édition : supprimées si on annule. */
    private val newPhotos = ArrayList<String>()

    // --- Galerie -----------------------------------------------------------------------------
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) toast(R.string.msg_photo_selection_cancelled) else applyNewPhoto(uri)
        }

    // --- Caméra ------------------------------------------------------------------------------
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val temp = cameraTempFile
            cameraTempFile = null
            if (success && temp != null && temp.exists() && temp.length() > 0) {
                applyNewPhoto(Uri.fromFile(temp))
            } else {
                toast(R.string.msg_photo_capture_cancelled)
            }
            temp?.delete()
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else toast(R.string.msg_camera_permission_denied)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_CONTACT_ID, ContactViewModel.NOUVEAU_CONTACT)
        if (savedInstanceState != null) {
            champsInitialises = savedInstanceState.getBoolean(STATE_INIT, false)
            currentPhotoPath = savedInstanceState.getString(STATE_PHOTO)
            cameraTempFile = savedInstanceState.getString(STATE_TEMP)?.let { File(it) }
            savedInstanceState.getStringArrayList(STATE_NEW_PHOTOS)?.let { newPhotos.addAll(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appliquerPreferences()
        binding.btnDelete.visibility = if (modeCreation) View.GONE else View.VISIBLE

        when {
            champsInitialises -> showPhoto() // saisie en cours conservée (rotation, retour de la Carte)
            modeCreation -> {                // création : champs vides
                champsInitialises = true
                showPhoto()
            }
            else -> chargerDepuisLaBase()
        }
        majResumePosition()

        // Efface le message d'erreur dès que l'utilisateur corrige le champ
        binding.editLastName.doAfterTextChanged { binding.layoutLastName.error = null }
        binding.editFirstName.doAfterTextChanged { binding.layoutFirstName.error = null }
        binding.editPhone.doAfterTextChanged { binding.layoutPhone.error = null }
        binding.editEmail.doAfterTextChanged { binding.layoutEmail.error = null }
        binding.editLatitude.doAfterTextChanged { binding.layoutLatitude.error = null; majResumePosition() }
        binding.editLongitude.doAfterTextChanged { binding.layoutLongitude.error = null; majResumePosition() }

        binding.btnCall.setOnClickListener { call() }
        binding.btnEmail.setOnClickListener { sendEmail() }
        binding.btnPickPhoto.setOnClickListener { pickFromGallery() }
        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnMap.setOnClickListener { voirSurLaCarte() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { cancel() }
        binding.btnDelete.setOnClickListener { confirmerSuppression() }

        // Bouton Retour (et flèche de la barre) = Annuler
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = cancel()
            })
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(
            if (modeCreation) R.string.title_new else R.string.title_detail
        )
    }

    /** Lecture du contact en base (coroutine, Dispatchers.IO dans le ViewModel). */
    private fun chargerDepuisLaBase() {
        binding.btnSave.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            if (_binding == null) return@launch
            if (contact == null) {
                toast(R.string.msg_invalid_contact)
                (requireActivity() as MainActivity).retour()
                return@launch
            }
            binding.editLastName.setText(contact.nom)
            binding.editFirstName.setText(contact.prenom)
            binding.editPhone.setText(contact.telephone)
            binding.editEmail.setText(contact.email.orEmpty())
            binding.editLatitude.setText(contact.latitude?.toString().orEmpty())
            binding.editLongitude.setText(contact.longitude?.toString().orEmpty())
            currentPhotoPath = contact.photoUri
            champsInitialises = true
            showPhoto()
            majResumePosition()
            binding.btnSave.isEnabled = true
        }
    }

    /** Taille (nom, prénom, téléphone) et couleur (nom, prénom) choisies dans les Paramètres. */
    private fun appliquerPreferences() {
        val p = PreferencesManager(requireContext()).lire()
        listOf(binding.editLastName, binding.editFirstName, binding.editPhone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, p.tailleSp)
        }
        binding.editLastName.setTextColor(p.couleur)
        binding.editFirstName.setTextColor(p.couleur)
    }

    // ===== Coordonnées GPS ======================================================================

    private fun majResumePosition() {
        val lat = Coordonnees.analyser(binding.editLatitude.text.toString(), Coordonnees.LAT_MIN, Coordonnees.LAT_MAX)
        val lng = Coordonnees.analyser(binding.editLongitude.text.toString(), Coordonnees.LNG_MIN, Coordonnees.LNG_MAX)
        binding.txtPosition.text = when {
            lat is Coordonnees.Resultat.Ok && lng is Coordonnees.Resultat.Ok ->
                getString(R.string.position_summary, lat.valeur, lng.valeur)
            lat is Coordonnees.Resultat.Vide && lng is Coordonnees.Resultat.Vide ->
                getString(R.string.position_none)
            else -> getString(R.string.position_invalid)
        }
    }

    /**
     * Valide les deux champs. Retourne (lat, lng), (null, null) si les deux sont vides,
     * ou null si la saisie est invalide (les erreurs sont affichées sur les champs).
     */
    private fun lireCoordonnees(): Pair<Double?, Double?>? {
        val lat = Coordonnees.analyser(binding.editLatitude.text.toString(), Coordonnees.LAT_MIN, Coordonnees.LAT_MAX)
        val lng = Coordonnees.analyser(binding.editLongitude.text.toString(), Coordonnees.LNG_MIN, Coordonnees.LNG_MAX)

        if (lat is Coordonnees.Resultat.Vide && lng is Coordonnees.Resultat.Vide) return Pair(null, null)

        var ok = true
        when (lat) {
            Coordonnees.Resultat.Vide -> { binding.layoutLatitude.error = getString(R.string.error_lat_missing); ok = false }
            Coordonnees.Resultat.NonNumerique -> { binding.layoutLatitude.error = getString(R.string.error_not_number); ok = false }
            Coordonnees.Resultat.HorsLimites -> { binding.layoutLatitude.error = getString(R.string.error_lat_range); ok = false }
            is Coordonnees.Resultat.Ok -> Unit
        }
        when (lng) {
            Coordonnees.Resultat.Vide -> { binding.layoutLongitude.error = getString(R.string.error_lng_missing); ok = false }
            Coordonnees.Resultat.NonNumerique -> { binding.layoutLongitude.error = getString(R.string.error_not_number); ok = false }
            Coordonnees.Resultat.HorsLimites -> { binding.layoutLongitude.error = getString(R.string.error_lng_range); ok = false }
            is Coordonnees.Resultat.Ok -> Unit
        }
        if (!ok) return null
        return Pair((lat as Coordonnees.Resultat.Ok).valeur, (lng as Coordonnees.Resultat.Ok).valeur)
    }

    private fun voirSurLaCarte() {
        val coords = lireCoordonnees()
        if (coords == null) {
            toast(R.string.msg_coords_invalid)
            return
        }
        val nom = "${binding.editLastName.text.toString().trim()} ${binding.editFirstName.text.toString().trim()}"
            .trim().ifEmpty { getString(R.string.title_new) }

        // Sans clé API Google Maps : on ouvre l'application Google Maps du téléphone
        // avec un Intent implicite (URI geo:), comme ACTION_DIAL pour le composeur. Aucune clé nécessaire.
        val lat = coords.first
        val lng = coords.second
        if (!BuildConfig.HAS_MAPS_KEY && lat != null && lng != null) {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(nom)})")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: ActivityNotFoundException) {
                // Aucune application de carte installée : on affiche l'écrate (message explicite)n Car
                (requireActivity() as MainActivity).ouvrirCarte(contactId, nom, lat, lng)
            }
            return
        }

        (requireActivity() as MainActivity).ouvrirCarte(contactId, nom, coords.first, coords.second)
    }

    // ===== Appeler / e-mail (Intents implicites) =================================================

    private fun call() {
        val phone = binding.editPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.layoutPhone.error = getString(R.string.error_phone_empty)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_dialer)
        }
    }

    private fun sendEmail() {
        val email = binding.editEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.layoutEmail.error = getString(R.string.error_email_empty)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_mail_app)
        }
    }

    // ===== Photo : galerie et caméra ============================================================

    private fun pickFromGallery() {
        try {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.msg_no_gallery)
        }
    }

    private fun takePhoto() {
        val ctx = requireContext()
        if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(R.string.msg_camera_unavailable)
            return
        }
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val temp = File(ImageUtils.photosDir(ctx), "tmp_${System.currentTimeMillis()}.jpg")
        cameraTempFile = temp
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", temp)
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraTempFile = null
            temp.delete()
            toast(R.string.msg_no_camera_app)
        } catch (e: SecurityException) {
            cameraTempFile = null
            temp.delete()
            toast(R.string.msg_camera_permission_denied)
        }
    }

    /**
     * Copie l'image (galerie ou caméra) dans filesDir/photos : c'est ce chemin qui sera enregistré
     * en base. Une URI de galerie peut devenir illisible après un redémarrage, la copie non.
     */
    private fun applyNewPhoto(source: Uri) {
        val ctx = context ?: return
        val dest = File(ImageUtils.photosDir(ctx), "photo_${System.nanoTime()}.jpg")
        if (!ImageUtils.importPhoto(ctx, source, dest)) {
            toast(R.string.msg_photo_load_failed)
            return
        }
        currentPhotoPath?.let { previous ->
            if (newPhotos.remove(previous)) ImageUtils.deleteQuietly(previous)
        }
        newPhotos.add(dest.path)
        currentPhotoPath = dest.path
        if (_binding != null) showPhoto()
    }

    private fun showPhoto() {
        val bitmap = currentPhotoPath?.let { ImageUtils.decodeScaled(it, 512) }
        if (bitmap != null) {
            binding.imgPhoto.setImageBitmap(bitmap)
        } else {
            binding.imgPhoto.setImageResource(R.drawable.placeholder_avatar)
        }
    }

    // ===== Enregistrer / Supprimer / Annuler ====================================================

    private fun save() {
        if (operationEnCours) return
        val nom = binding.editLastName.text.toString().trim()
        val prenom = binding.editFirstName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // Validation : rien n'est écrit en base tant qu'un champ obligatoire est invalide
        var firstInvalid: View? = null
        if (nom.isEmpty()) {
            binding.layoutLastName.error = getString(R.string.error_last_name)
            firstInvalid = binding.editLastName
        }
        if (prenom.isEmpty()) {
            binding.layoutFirstName.error = getString(R.string.error_first_name)
            firstInvalid = firstInvalid ?: binding.editFirstName
        }
        if (phone.isEmpty()) {
            binding.layoutPhone.error = getString(R.string.error_phone_empty)
            firstInvalid = firstInvalid ?: binding.editPhone
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.layoutPhone.error = getString(R.string.error_phone_invalid)
            firstInvalid = firstInvalid ?: binding.editPhone
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            firstInvalid = firstInvalid ?: binding.editEmail
        }
        val coords = lireCoordonnees()
        if (coords == null) {
            firstInvalid = firstInvalid
                ?: if(binding.layoutLatitude.error != null) binding.editLatitude else binding.editLongitude
        }
        if (firstInvalid != null || coords == null) {
            firstInvalid?.requestFocus()
            return
        }

        val contact = Contact(
            id = contactId,
            nom = nom,
            prenom = prenom,
            telephone = phone,
            email = email.ifEmpty { null },
            photoUri = currentPhotoPath,
            latitude = coords.first,
            longitude = coords.second
        )

        operationEnCours = true
        binding.btnSave.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val resultat = viewModel.enregistrer(contact)
            operationEnCours = false
            if (_binding == null) return@launch
            when (resultat) {
                is ContactViewModel.Resultat.Insere, ContactViewModel.Resultat.MisAJour -> {
                    // On garde uniquement la photo finale parmi celles créées pendant l'édition
                    newPhotos.filter { it != currentPhotoPath }.forEach { ImageUtils.deleteQuietly(it) }
                    newPhotos.clear()
                    toast(R.string.msg_saved)
                    (requireActivity() as MainActivity).retour()
                }
                ContactViewModel.Resultat.Introuvable -> {
                    toast(R.string.msg_invalid_contact)
                    binding.btnSave.isEnabled = true
                }
                ContactViewModel.Resultat.Echec -> {
                    toast(R.string.msg_save_failed)
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    private fun confirmerSuppression() {
        if (operationEnCours) return
        val nom = "${binding.editLastName.text} ${binding.editFirstName.text}".trim()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, nom))
            .setPositiveButton(R.string.btn_delete) { _, _ -> supprimer() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun supprimer() {
        operationEnCours = true
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = viewModel.supprimer(contactId)
            operationEnCours = false
            if (_binding == null) return@launch
            if (ok) {
                newPhotos.forEach { ImageUtils.deleteQuietly(it) }
                newPhotos.clear()
                toast(R.string.msg_deleted)
                (requireActivity() as MainActivity).retour()
            } else {
                toast(R.string.msg_delete_failed)
            }
        }
    }

    /** Retour à la liste sans enregistrer : la base n'est pas modifiée. */
    private fun cancel() {
        if (operationEnCours) return
        newPhotos.forEach { ImageUtils.deleteQuietly(it) }
        newPhotos.clear()
        cameraTempFile?.delete()
        cameraTempFile = null
        (requireActivity() as MainActivity).retour()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_INIT, champsInitialises)
        outState.putString(STATE_PHOTO, currentPhotoPath)
        outState.putString(STATE_TEMP, cameraTempFile?.path)
        outState.putStringArrayList(STATE_NEW_PHOTOS, newPhotos)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toast(resId: Int) {
        context?.let { Toast.makeText(it, resId, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        const val TAG = "detail"
        private const val ARG_CONTACT_ID = "contact_id"
        private const val STATE_INIT = "state_init"
        private const val STATE_PHOTO = "state_photo"
        private const val STATE_TEMP = "state_temp"
        private const val STATE_NEW_PHOTOS = "state_new_photos"

        /** Seul l'_id est transmis (Bundle) : le contact est relu en base. */
        fun newInstance(contactId: Long) = DetailContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_CONTACT_ID, contactId) }
        }
    }
}
