package com.example.mescontacts.data

import com.example.mescontacts.Coordonnees

/**
 * Modèle d'un contact.
 * [id] vaut 0 tant que le contact n'est pas enregistré, puis prend la valeur _id attribuée par la base.
 * [photoUri] : chemin d'une copie de l'image dans le stockage interne (filesDir), ou null.
 * [latitude] / [longitude] : null = aucune position.
 */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val nomComplet: String get() = "$nom $prenom".trim()

    val aUnePosition: Boolean get() = Coordonnees.sontValides(latitude, longitude)
}
