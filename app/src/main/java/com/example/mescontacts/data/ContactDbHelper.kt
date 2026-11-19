package com.example.mescontacts.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mescontacts.data.ContactContract.COL_EMAIL
import com.example.mescontacts.data.ContactContract.COL_LATITUDE
import com.example.mescontacts.data.ContactContract.COL_LONGITUDE
import com.example.mescontacts.data.ContactContract.COL_NOM
import com.example.mescontacts.data.ContactContract.COL_PRENOM
import com.example.mescontacts.data.ContactContract.COL_TELEPHONE
import com.example.mescontacts.data.ContactContract.TABLE

/**
 * Création et mise à jour de la base contacts.db.
 * Instance unique (singleton) construite avec applicationContext : une seule connexion
 * partagée par toute l'application, sans retenir une Activity en mémoire.
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, ContactContract.DATABASE_NAME, null, DATABASE_VERSION) {

    /** Appelé une seule fois, quand le fichier contacts.db n'existe pas encore. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ContactContract.SQL_CREATE)
        insererDemonstration(db)
    }

    /**
     * Appelé quand DATABASE_VERSION augmente. En version 1 il n'existe aucune ancienne
     * version à migrer : on recrée la table. Une vraie migration utiliserait ALTER TABLE.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ContactContract.SQL_DROP)
        onCreate(db)
    }

    /** 6 contacts de démonstration, dont 2 sans coordonnées GPS. */
    private fun insererDemonstration(db: SQLiteDatabase) {
        val demos = listOf(
            Contact(nom = "Dupont", prenom = "Claire", telephone = "06 12 34 56 78",
                email = "claire.dupont@example.com", latitude = 48.8584, longitude = 2.2945),
            Contact(nom = "Martin", prenom = "Thomas", telephone = "06 98 45 63 20",
                email = "thomas.martin@example.com", latitude = 36.8065, longitude = 10.1815),
            Contact(nom = "Bernard", prenom = "Sophie", telephone = "06 60 71 24 32",
                email = "sophie.bernard@example.com", latitude = 35.8256, longitude = 10.6084),
            Contact(nom = "Petit", prenom = "Lucas", telephone = "07 11 22 85 44",
                email = "lucas.petit@example.com", latitude = 34.7406, longitude = 10.7603),
            Contact(nom = "Moreau", prenom = "Julien", telephone = "06 55 44 93 22",
                email = "julien.moreau@example.com"),
            Contact(nom = "Ben Salah", prenom = "Amira", telephone = "+216 98 765 432",
                email = "amira.bensalah@example.com")
        )
        db.beginTransaction()
        try {
            for (c in demos) {
                val values = ContentValues().apply {
                    put(COL_NOM, c.nom)
                    put(COL_PRENOM, c.prenom)
                    put(COL_TELEPHONE, c.telephone)
                    put(COL_EMAIL, c.email)
                    put(COL_LATITUDE, c.latitude)
                    put(COL_LONGITUDE, c.longitude)
                }
                db.insert(TABLE, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
