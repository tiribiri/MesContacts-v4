package com.example.mescontacts.data

/**
 * Noms de la table et des colonnes, regroupés ici.
 * Aucun nom de colonne n'est écrit en dur ailleurs dans l'application.
 */
object ContactContract {

    const val DATABASE_NAME = "contacts.db"
    const val TABLE = "contacts"

    const val COL_ID = "_id"
    const val COL_NOM = "nom"
    const val COL_PRENOM = "prenom"
    const val COL_TELEPHONE = "telephone"
    const val COL_EMAIL = "email"
    const val COL_PHOTO_URI = "photo_uri"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"

    /** Le téléphone est en TEXT : il peut commencer par 0 ou +216 et contenir des espaces. */
    const val SQL_CREATE = """
        CREATE TABLE $TABLE (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NOM TEXT NOT NULL,
            $COL_PRENOM TEXT NOT NULL,
            $COL_TELEPHONE TEXT NOT NULL,
            $COL_EMAIL TEXT,
            $COL_PHOTO_URI TEXT,
            $COL_LATITUDE REAL,
            $COL_LONGITUDE REAL
        )
    """

    const val SQL_DROP = "DROP TABLE IF EXISTS $TABLE"

    /** Tri par nom puis prénom, sans tenir compte de la casse. */
    const val ORDER_BY = "$COL_NOM COLLATE NOCASE, $COL_PRENOM COLLATE NOCASE"
}
