package dev.aa55h.gtfs.lines

import dev.aa55h.gtfs.encodeFormData
import dev.aa55h.gtfs.mediaTypeFormUrlEncoded
import dev.aa55h.gtfs.okHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val url = "https://portal.radekpapez.cz/"

val transportOptions = mapOf(
    "30022" to 30022,
    "30024" to 30024,
    "30025" to 30025,
    "30061" to 30061,
    "30073" to 30073,
    "30079" to 30079,
    "30097" to 30097,
    "30206" to 30206,
    "30213" to 30213,
    "Doprava Ústeckého kraje" to 30421,
    "IDS Jihomoravského kraje" to 30621,
    "IDS Libereckého kraje" to 30512,
    "IDS Moravskoslezského kraje" to 30811,
    "Integrovaná doprava Plzeňska" to 30321,
    "Integrovaná doprava Zlínského kraje" to 30722,
    "Integrovaná regionální doprava Královéhradeckého a Pardubického kraje" to 30522,
    "MHD v Bílině" to 30202,
    "MHD v Bruntálu" to 30042,
    "MHD v Českém Těšíně" to 30017,
    "MHD v Českých Budějovicích" to 30004,
    "MHD v Duchcově" to 30209,
    "MHD v Havířově" to 30023,
    "MHD v Chebu" to 30057,
    "MHD v Chrudimi" to 30015,
    "MHD v Jáchymově" to 30069,
    "MHD v Jihlavě" to 30041,
    "MHD v Kadani" to 30211,
    "MHD v Karlových Varech" to 30036,
    "MHD v Litomyšli" to 30065,
    "MHD v Novém Městě na Moravě" to 30220,
    "MHD v Olomouci" to 30005,
    "MHD v Ostrově" to 30091,
    "MHD v Plzni" to 30007,
    "MHD v Poličce" to 30046,
    "MHD v Přelouči" to 30083,
    "MHD v Přerově" to 30026,
    "MHD v Roudnici nad Labem" to 30093,
    "MHD v Šumperku" to 30048,
    "MHD v Teplicích" to 30040,
    "MHD v Trutnově" to 30016,
    "MHD v Třinci" to 30018,
    "MHD v Ústí nad Labem" to 30044,
    "MHD v Zábřehu" to 30049,
    "MHD ve Studénce" to 30051,
    "MHD ve Vsetíně" to 30062,
    "MHD ve Žďáru nad Sázavou" to 30028,
    "Pražská integrovaná doprava" to 30001
)

private fun getRequestBodyWithOperators(operators: List<String>): RequestBody {
    val raw = """
        cislo=
        koddopravy=
        cisloids=
        nazev=
        check_dopravce_ano=on
        ${operators.joinToString("\n") { id -> "dopravci_ano[]=$id" }}
        dopravci_ne[]=
        dopravci_ne[]=
        dopravci_ne[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        rezim=AND
        datum1=2025-08-06
        datum2=2025-08-06
        datum_od=2025-08-06
        datum_do=2025-08-06
        vyluka=vse
        hledani=Hledat
    """.trimIndent()
    
    return encodeFormData(raw).toRequestBody(mediaTypeFormUrlEncoded)
}

private fun getRequestBodyWithIds(ids: Int): RequestBody {
    val raw = """
        cislo=
        check_koddopravy=on
        koddopravy=$ids
        cisloids=
        nazev=
        check_dopravce_ano=on
        dopravci_ano[]=
        dopravci_ano[]=
        dopravci_ano[]=
        dopravci_ano[]=
        dopravci_ano[]=
        dopravci_ne[]=
        dopravci_ne[]=
        dopravci_ne[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        zastavky[]=
        rezim=AND
        datum1=2025-08-06
        datum2=2025-08-06
        datum_od=2025-08-06
        datum_do=2025-08-06
        vyluka=vse
        hledani=Hledat
    """.trimIndent()

    return encodeFormData(raw).toRequestBody(mediaTypeFormUrlEncoded)
}

private fun requestDocument(body: RequestBody): Document {
    val request = okhttp3.Request.Builder()
        .url(url)
        .post(body)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
        .build()
    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw Exception("Unexpected code $response")
        }
        return Jsoup.parse(response.body.string())
    }
}

private fun parseDocument(document: Document): List<Line> {
    return document.select("table#vysledky tr").mapNotNull { row ->
        val cells = row.select("td")
        if (cells.size < 3) return@mapNotNull null
        Line(
            line = cells[0].text().toIntOrNull() ?: return@mapNotNull null,
            ids = cells[1].text(),
            name = cells[2].text(),
            operator = cells.getOrNull(3)?.text() ?: ""
        )
    }
}

/**
 * Fetches Czech lines based on the provided operators and IDS numbers(which can be found in the `transportOptions` map).
 * The operation between operators and IDSs is an OR operation, meaning multiple requests could be made.
 */
fun fetchLines(operators: List<String>, ids: List<Int> = emptyList()): List<Line> {
    val list = mutableListOf<Line>()
    
    if (operators.isNotEmpty()) {
        val body = getRequestBodyWithOperators(operators)
        val document = requestDocument(body)
        list.addAll(parseDocument(document))
    }
    
    list.addAll(ids.map {
        val body = getRequestBodyWithIds(it)
        val document = requestDocument(body)
        parseDocument(document)
    }.reduceOrNull { acc, lines -> acc + lines} ?: emptyList())
    
    return list
}

data class Line(
    val line: Int,
    val ids: String,
    val name: String,
    val operator: String
)