package com.example.mescontacts.data

import android.content.ContentValues
import android.database.Cursor
import com.example.mescontacts.data.ContactContract.COL_EMAIL
import com.example.mescontacts.data.ContactContract.COL_ID
import com.example.mescontacts.data.ContactContract.COL_LATITUDE
import com.example.mescontacts.data.ContactContract.COL_LONGITUDE
import com.example.mescontacts.data.ContactContract.COL_NOM
import com.example.mescontacts.data.ContactContract.COL_PHOTO_URI
import com.example.mescontacts.data.ContactContract.COL_PRENOM
import com.example.mescontacts.data.ContactContract.COL_TELEPHONE
import com.example.mescontacts.data.ContactContract.ORDER_BY
import com.example.mescontacts.data.ContactContract.TABLE

/**
 * Seule classe qui contient du SQL.
 * Toutes les valeurs variables passent par des arguments « ? » (aucune concaténation de saisie).
 * Les méthodes sont bloquantes : elles sont appelées uniquement depuis Dispatchers.IO (ContactViewModel).
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** Retourne le nouvel _id, ou -1 en cas d'échec. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(TABLE, null, toValues(contact))

    /** Retourne le nombre de lignes modifiées (1 si OK, 0 si le contact n'existe plus). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(TABLE, toValues(contact), "$COL_ID = ?", arrayOf(contact.id.toString()))

    /** Retourne le nombre de lignes supprimées. */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> = query(null, null)

    fun getById(id: Long): Contact? = query("$COL_ID = ?", arrayOf(id.toString())).firstOrNull()

    /**
     * Recherche (LIKE, insensible à la casse pour les lettres non accentuées) sur le nom, le prénom,
     * « nom prénom », « prénom nom » et le téléphone (avec et sans espaces).
     */
    fun search(texte: String): List<Contact> {
        if (texte.isBlank()) return getAll()
        val m = RechercheSql.motif(texte)
        val tel = RechercheSql.motifSansEspaces(texte)
        val esc = "ESCAPE '${RechercheSql.ESCAPE}'"
        val selection = """
            $COL_NOM LIKE ? $esc
            OR $COL_PRENOM LIKE ? $esc
            OR ($COL_NOM || ' ' || $COL_PRENOM) LIKE ? $esc
            OR ($COL_PRENOM || ' ' || $COL_NOM) LIKE ? $esc
            OR $COL_TELEPHONE LIKE ? $esc
            OR REPLACE($COL_TELEPHONE, ' ', '') LIKE ? $esc
        """.trimIndent()
        return query(selection, arrayOf(m, m, m, m, m, tel))
    }

    private fun query(selection: String?, args: Array<String>?): List<Contact> =
        helper.readableDatabase.query(TABLE, null, selection, args, null, null, ORDER_BY)
            .use { cursor -> lire(cursor) }

    private fun lire(cursor: Cursor): List<Contact> {
        val iId = cursor.getColumnIndexOrThrow(COL_ID)
        val iNom = cursor.getColumnIndexOrThrow(COL_NOM)
        val iPrenom = cursor.getColumnIndexOrThrow(COL_PRENOM)
        val iTel = cursor.getColumnIndexOrThrow(COL_TELEPHONE)
        val iEmail = cursor.getColumnIndexOrThrow(COL_EMAIL)
        val iPhoto = cursor.getColumnIndexOrThrow(COL_PHOTO_URI)
        val iLat = cursor.getColumnIndexOrThrow(COL_LATITUDE)
        val iLng = cursor.getColumnIndexOrThrow(COL_LONGITUDE)

        val liste = ArrayList<Contact>(cursor.count)
        while (cursor.moveToNext()) {
            liste.add(
                Contact(
                    id = cursor.getLong(iId),
                    nom = cursor.getString(iNom),
                    prenom = cursor.getString(iPrenom),
                    telephone = cursor.getString(iTel),
                    email = if (cursor.isNull(iEmail)) null else cursor.getString(iEmail),
                    photoUri = if (cursor.isNull(iPhoto)) null else cursor.getString(iPhoto),
                    latitude = if (cursor.isNull(iLat)) null else cursor.getDouble(iLat),
                    longitude = if (cursor.isNull(iLng)) null else cursor.getDouble(iLng)
                )
            )
        }
        return liste
    }

    /** L'_id n'est jamais écrit : il est attribué par la base (AUTOINCREMENT). */
    private fun toValues(c: Contact) = ContentValues().apply {
        put(COL_NOM, c.nom)
        put(COL_PRENOM, c.prenom)
        put(COL_TELEPHONE, c.telephone)
        put(COL_EMAIL, c.email?.takeIf { it.isNotBlank() })
        put(COL_PHOTO_URI, c.photoUri)
        put(COL_LATITUDE, c.latitude)   // null → NULL en base
        put(COL_LONGITUDE, c.longitude)
    }
}
