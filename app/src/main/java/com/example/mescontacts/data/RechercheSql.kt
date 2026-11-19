package com.example.mescontacts.data

/**
 * Préparation des motifs LIKE pour la recherche (fonctions pures, testées dans RechercheSqlTest).
 * La valeur est toujours passée en argument « ? » : seuls % et _ doivent être échappés,
 * pour être cherchés comme de vrais caractères. L'apostrophe ne pose aucun problème.
 */
object RechercheSql {

    const val ESCAPE = '\\'

    /** « dup » → « %dup% » ; « 50% » → « %50\%% ». */
    fun motif(texte: String): String = "%" + echapper(texte.trim()) + "%"

    /** Motif sans espaces, pour retrouver « 06 12 » dans « 0612… » et inversement. */
    fun motifSansEspaces(texte: String): String = "%" + echapper(texte.replace(" ", "")) + "%"

    fun echapper(s: String): String = buildString {
        for (ch in s) {
            if (ch == ESCAPE || ch == '%' || ch == '_') append(ESCAPE)
            append(ch)
        }
    }
}
