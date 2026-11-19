package com.example.mescontacts

import com.example.mescontacts.data.RechercheSql
import org.junit.Assert.assertEquals
import org.junit.Test

class RechercheSqlTest {

    @Test fun motifSimple() = assertEquals("%dup%", RechercheSql.motif("dup"))
    @Test fun espacesAuxBords() = assertEquals("%dup%", RechercheSql.motif("  dup "))
    @Test fun apostropheInchangee() = assertEquals("%O'Neil%", RechercheSql.motif("O'Neil"))
    @Test fun pourcentEchappe() = assertEquals("%50\\%%", RechercheSql.motif("50%"))
    @Test fun soulignementEchappe() = assertEquals("%a\\_b%", RechercheSql.motif("a_b"))
    @Test fun antislashEchappe() = assertEquals("%a\\\\b%", RechercheSql.motif("a\\b"))
    @Test fun telephoneSansEspaces() = assertEquals("%0612%", RechercheSql.motifSansEspaces("06 12"))
}
