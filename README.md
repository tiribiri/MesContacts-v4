# Mes contacts – V4 (SQLite + préférences)

Application Android (Kotlin + XML) de gestion de contacts. La V4 reprend la V3
(fragments, RecyclerView filtrable, Google Maps) et ajoute :

- une base **SQLite** `contacts.db` (API native `SQLiteOpenHelper`, sans Room) : les contacts sont conservés
  après fermeture de l'application et redémarrage du téléphone ;
- le **CRUD** complet : bouton « + », modification, suppression avec confirmation ;
- un écran **Paramètres** (AndroidX Preference) : taille de police, couleur du texte, bouton Appeler sur chaque ligne.

## Architecture
```
MainActivity (barre d'application + FragmentContainerView)
 ├─ ListeContactsFragment   recherche, RecyclerView, « + », Appeler par ligne, menu Paramètres
 ├─ DetailContactFragment   création / modification / suppression, Appeler, E-mail, Galerie, Caméra, Carte
 ├─ CarteContactFragment    Google Maps
 └─ ParametresFragment      PreferenceFragmentCompat (res/xml/preferences.xml)

ContactViewModel (partagé)  viewModelScope + Dispatchers.IO
 └─ data/ContactDao         seule classe contenant du SQL (requêtes paramétrées « ? »)
     └─ data/ContactDbHelper  SQLiteOpenHelper singleton (applicationContext)
PreferencesManager          lecture et conversion des SharedPreferences
```

## Clé Google Maps (non publiée)
Ajouter dans **`local.properties`** (ignoré par Git) :
```
MAPS_API_KEY=VOTRE_CLE_ICI
```

## Installation
1. Cloner le dépôt, l'ouvrir dans Android Studio et synchroniser Gradle.
2. Ajouter `MAPS_API_KEY` dans `local.properties`.
3. Lancer sur un émulateur avec Google Play ou un appareil réel.

La table peut être consultée avec **App Inspection → Database Inspector**.

## Tests unitaires
`./gradlew test` : `RechercheSqlTest` (motifs LIKE, apostrophe, % et _) et `CoordonneesTest`.

## Auteur
Triki Mohamed Amine – L3MDW2
