package com.eavorari.data

import com.eavorari.data.model.BusArea
import com.eavorari.data.model.BusLine
import com.eavorari.data.model.ExtraBusRoute
import com.eavorari.data.model.MetroLine
import com.eavorari.data.model.MetroStop
import com.eavorari.data.model.Station
import com.eavorari.data.model.StrikeInfo
import com.eavorari.data.model.TrainInfo
import com.eavorari.data.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EavRepository {
    private var strikeCache: List<StrikeInfo>? = null
    private var strikeCacheDate: String? = null

    private val baseUrl = "https://orariotreni.eavsrl.it"

    suspend fun fetchStations(): List<Station> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(baseUrl).timeout(15000).get()
        val scripts = doc.select("script")
        for (script in scripts) {
            val html = script.html()
            if (html.contains("var xml = ")) {
                val jsonStart = html.indexOf('{')
                val jsonEnd = html.lastIndexOf('}') + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val raw = html.substring(jsonStart, jsonEnd)
                    val root = JSONObject(raw)
                    val impianti = root.getJSONArray("impianto")
                    val seen = mutableSetOf<String>()
                    val stations = mutableListOf<Station>()
                    for (i in 0 until impianti.length()) {
                        val item = impianti.getJSONObject(i)
                        if (item.optString("visualizzato", "true") != "true") continue
                        val id = item.getString("id")
                        val name = item.getString("descrizione")
                        val lineId = item.optString("idLinea", "")
                        val lineName = item.optString("linea", "")
                        if (seen.add(id)) {
                            stations.add(Station(id, name, lineId, lineName))
                        }
                    }
                    return@withContext stations.sortedBy { it.name }
                }
            }
        }
        throw Exception("Impossibile trovare i dati delle stazioni")
    }

    suspend fun fetchSchedule(stationId: String, tipo: String): UiState = withContext(Dispatchers.IO) {
        try {
            val device = "M01T${stationId}M"
            val doc = Jsoup.connect("$baseUrl/teleindicatori/ws_getData.php")
                .method(org.jsoup.Connection.Method.POST)
                .data("device", device)
                .data("tipoLista", tipo)
                .data("codLoc", stationId)
                .data("visualizzazione", "mobile")
                .timeout(15000)
                .execute()
                .parse()

            val stationName = doc.selectFirst("td.nomeLocalita")?.text() ?: "Sconosciuta"

            val trains = mutableListOf<TrainInfo>()
            val rows = doc.select("tr.trenoRow, tr[class~=testoGiallo]")
            for (row in rows) {
                val numTreno = row.selectFirst("td.numTreno")?.text()?.trim() ?: ""
                val categoria = row.selectFirst("td.categoria")?.text()?.trim() ?: ""
                val destinazione = row.selectFirst("td.destinazione, td > div.destinazione")?.text()?.trim() ?: ""
                val informazioni = row.selectFirst("td.informazioni")?.text()?.trim() ?: ""
                val binario = row.selectFirst("td.binario")?.text()?.trim() ?: ""
                val orario = row.selectFirst("td.orario")?.text()?.trim() ?: ""
                val ritardo = row.selectFirst("td.ritardo")?.text()?.trim() ?: ""

                if (numTreno.isNotBlank() && orario.isNotBlank()) {
                    trains.add(TrainInfo(numTreno, categoria, destinazione, informazioni, binario, orario, ritardo))
                }
            }

            UiState.Success(stationName, tipo, trains, System.currentTimeMillis())
        } catch (e: Exception) {
            UiState.Error("Errore: ${e.message ?: "connessione fallita"}")
        }
    }

    fun getBusAreas(): List<BusArea> = listOf(
        BusArea("Costiera Sorrentina", listOf(
            BusLine("007", "Piano – Sant'Agata", "Sorrentina", "https://www.eavsrl.it/download/007-piano-santagata/"),
            BusLine("008", "Meta – Massalubrense", "Sorrentina", "https://www.eavsrl.it/download/008-meta-massalubrense/"),
            BusLine("008D", "Sorrento – Marina Grande (Circ. D)", "Sorrentina", "https://www.eavsrl.it/download/008-sorrento-marina-grande-circolari-d/"),
            BusLine("009", "Circolari Vico Equense", "Sorrentina", "https://www.eavsrl.it/download/009-circolari-vico-equense/"),
            BusLine("009F", "Vico Equense – Monte Faito", "Sorrentina", "https://www.eavsrl.it/download/009-vico-equense-monte-faito/"),
        )),
        BusArea("Flegrea", listOf(
            BusLine("101", "Monte di Procida – Napoli", "Flegrea", "https://www.eavsrl.it/download/101-monte-di-procida-napoli-dal-2-03-2026/"),
            BusLine("106", "P.le Tecchio – Monteruscello", "Flegrea", "https://www.eavsrl.it/download/106-piazzale-tecchio-monteruscello/"),
            BusLine("911", "Lago Patria – Licola – Napoli", "Flegrea", "https://www.eavsrl.it/download/911-lago-patria-licola-napoli-p-le-tecchio/"),
        )),
        BusArea("Vesuviana – Nolana", listOf(
            BusLine("001", "Scafati – Napoli", "Vesuviana", "https://www.eavsrl.it/download/001-scafati-napoli/"),
            BusLine("002", "Napoli – Nola – Monteforte", "Vesuviana", "https://www.eavsrl.it/download/002-napoli-nola-monteforte/"),
            BusLine("005", "Nola – Castellammare", "Vesuviana", "https://www.eavsrl.it/download/005-nola-castellammare/"),
            BusLine("010", "Napoli – Carbonara – Sarno", "Vesuviana", "https://www.eavsrl.it/download/010-napoli-carbonara-sarno/"),
            BusLine("808", "Pompei – Vesuvio", "Vesuviana", "https://www.eavsrl.it/download/808-pompei-vesuvio/"),
            BusLine("903", "Acerra – Napoli", "Vesuviana", "https://www.eavsrl.it/download/903-acerra-napoli/"),
        )),
        BusArea("Castellammare – Torre", listOf(
            BusLine("301–307", "Linee Urbane Castellammare", "Torrese", "https://www.eavsrl.it/orari-autobus/"),
            BusLine("803–807", "Linee Urbane Torre del Greco", "Torrese", "https://www.eavsrl.it/orari-autobus/"),
        )),
        BusArea("Ischia e Procida", listOf(
            BusLine("171–199", "Linee Ischia", "Isole", "https://www.eavsrl.it/orari-autobus-ischia-e-procida/"),
            BusLine("137–142", "Linee Procida", "Isole", "https://www.eavsrl.it/orari-autobus-ischia-e-procida/"),
        )),
    )

    fun getNightBusAreas(): List<BusArea> = listOf(
        BusArea("Notturni Vesuviani", listOf(
            BusLine("N1", "Napoli – Sorrento (notturno)", "Notturni", "https://www.eavsrl.it/downloads/orari-linee-ferroviarie-orari-linee-vesuviane-servizi-autobus-a-supporto-notturni/"),
            BusLine("N2", "Napoli – Sarno (notturno)", "Notturni", "https://www.eavsrl.it/downloads/orari-linee-ferroviarie-orari-linee-vesuviane-servizi-autobus-a-supporto-notturni/"),
            BusLine("N3", "Napoli – Poggiomarino (notturno)", "Notturni", "https://www.eavsrl.it/downloads/orari-linee-ferroviarie-orari-linee-vesuviane-servizi-autobus-a-supporto-notturni/"),
            BusLine("N4", "Napoli – Baiano (notturno)", "Notturni", "https://www.eavsrl.it/downloads/orari-linee-ferroviarie-orari-linee-vesuviane-servizi-autobus-a-supporto-notturni/"),
        )),
        BusArea("Info Notturni", listOf(
            BusLine("?", "Servizi bus sostitutivi linee ferroviarie", "Notturni",
                "https://www.eavsrl.it/downloads/orari-linee-ferroviarie-orari-linee-vesuviane-servizi-autobus-a-supporto-notturni/"),
        )),
    )

    fun getExtraBusRoutes(): List<ExtraBusRoute> = listOf(
        ExtraBusRoute(
            operator = "CURRERI VIAGGI",
            routeName = "Sorrento → Napoli Aeroporto",
            stops = listOf(
                "Sorrento (Piazza De Curtis — stazione)",
                "Sant'Agnello (Corso Italia 123)",
                "Piano di Sorrento (Corso Italia 39 — Siesta)",
                "Meta (Piazza Santa Maria del Lauro)",
                "Vico Equense (Corso Filangieri 85)",
                "Castellammare (Nuove Terme)",
                "Castellammare (Viale Europa — Villa Stabia)",
                "Pompei (Piazza Porta Marina Inferiore)*",
                "Napoli Aeroporto (lato partenze)",
            ),
            returnStops = listOf(
                "Napoli Aeroporto (parcheggio bus)",
                "Pompei (Piazza Porta Marina Inferiore)*",
                "Castellammare (Viale Europa — Villa Stabia)",
                "Castellammare (Nuove Terme)",
                "Vico Equense (Corso Filangieri 50)",
                "Meta (Piazza Santa Maria del Lauro)",
                "Piano di Sorrento (Corso Italia angolo Via S. Sergio)",
                "Sant'Agnello (Piazza Matteotti)",
                "Sorrento (Piazza De Curtis — stazione)",
            ),
            departures = listOf("06:30", "08:30", "09:00", "10:30", "12:00", "14:00", "15:00", "16:30"),
            returns = listOf("09:00", "10:30", "11:00", "12:00", "13:00", "14:30", "16:30", "18:00", "19:30"),
            websiteUrl = "https://www.curreriviaggi.it/airport-shuttle/times-download",
            info = "Navetta aeroportuale. €10 sola andata, €18 A/R. Biglietto a bordo o online. *Pompei solo su prenotazione. Non opera 1 Gen e 25 Dic.",
            color = 0xFFF5A623,
        ),
        ExtraBusRoute(
            operator = "MAROZZI VIAGGI",
            routeName = "Sorrento → Roma Tiburtina",
            stops = listOf(
                "Sorrento (Corso Italia 259)",
                "Sant'Agnello (Corso Italia)",
                "Meta (Corso Italia)",
                "Piano di Sorrento (Corso Italia)",
                "Vico Equense (Piazza Kennedy)",
                "Castellammare di Stabia (Viale Europa)",
                "Pompei Scavi (Piazza Esedra)",
                "Napoli (Molo Beverello) — solo corsa 17:00",
                "Roma Tiburtina (Largo Guido Mazzoni)",
            ),
            returnStops = listOf(
                "Roma Tiburtina (Largo Guido Mazzoni)",
                "Napoli (Molo Beverello) — corsa 07:00",
                "Pompei Scavi (Piazza Esedra)",
                "Castellammare di Stabia (Viale Europa)",
                "Vico Equense (Piazza Kennedy)",
                "Piano di Sorrento (Corso Italia)",
                "Meta (Corso Italia)",
                "Sant'Agnello (Corso Italia)",
                "Sorrento (Corso Italia 259)",
            ),
            departures = listOf("06:00 (lun/mar/gio/ven)", "17:00 (tutti i giorni)"),
            returns = listOf("07:00 (sab/dom/festivi)", "15:00 (tutti i giorni)"),
            websiteUrl = "https://www.marozzivt.it/tratte/",
            info = "Durata ~4h. Biglietto online o in autostazione. Corsa 06:00 non attiva mercoledì e sabato. Corsa 07:00 da Roma attiva sab/dom/festivi.",
            color = 0xFF00D4FF,
        ),
        ExtraBusRoute(
            operator = "ITABUS (Italo)",
            routeName = "Sorrento → Napoli (Afragola AV)",
            stops = listOf(
                "Sorrento (Corso Italia 259)",
                "Sant'Agnello (Corso Italia)",
                "Meta (Corso Italia)",
                "Piano di Sorrento (Corso Italia)",
                "Vico Equense (Corso Filangieri)",
                "Castellammare di Stabia (Viale Europa)",
                "Pompei Scavi (Piazza Esedra)",
                "Napoli Afragola (stazione Alta Velocità Italo)",
                "Napoli Metropark Centrale (Corso Arnaldo Lucci)",
            ),
            returnStops = listOf(
                "Napoli Metropark Centrale (Corso Arnaldo Lucci)",
                "Napoli Afragola (stazione Alta Velocità Italo)",
                "Pompei Scavi (Piazza Esedra)",
                "Castellammare di Stabia (Viale Europa)",
                "Vico Equense (Corso Filangieri)",
                "Piano di Sorrento (Corso Italia)",
                "Meta (Corso Italia)",
                "Sant'Agnello (Corso Italia)",
                "Sorrento (Corso Italia 259)",
            ),
            departures = listOf("11:05", "16:50"),
            returns = listOf("08:50", "10:45"),
            websiteUrl = "https://www.itabus.it/it/autobus/sorrento",
            info = "Bus + Treno Italo con biglietto unico. Da Afragola raggiungi Roma, Milano, Firenze, Bologna. 2 corse/giorno. Il bus ferma lungo la SS145 con fermate nelle principali località della penisola. Biglietto su italotreno.com o itabus.it.",
            color = 0xFF7C3AED,
        ),
    )

    fun getSitaAreas(): List<BusArea> = listOf(
        BusArea("SITA Costiera", listOf(
            BusLine("5070", "Sorrento – Positano – Amalfi", "SITA",
                "https://sitasudtrasporti.it/campania/wp-content/uploads/sites/3/2026/03/ORARI-COSTIERA_A1.pdf"),
            BusLine("5070 dir.", "Sorrento – Massa Lubrense – Sant'Agata", "SITA",
                "https://sitasudtrasporti.it/campania/wp-content/uploads/sites/3/2026/03/ORARI-COSTIERA_A1.pdf"),
            BusLine("5070 loc.", "Massa Lubrense – Marina Lobra – Termini", "SITA",
                "https://sitasudtrasporti.it/campania/wp-content/uploads/sites/3/2026/03/ORARI-COSTIERA_A1.pdf"),
        )),
    )

    fun getMetroLines(): List<MetroLine> = listOf(
        MetroLine(
            name = "Linea 1",
            colorName = "Gialla",
            colorHex = 0xFFFFD700,
            operator = "ANM",
            stops = listOf(
                MetroStop("Piscinola / Aversa", "Linea 11", "capolinea nord"),
                MetroStop("Chiaiano", "", ""),
                MetroStop("Frullone", "", ""),
                MetroStop("Colli Aminei", "", ""),
                MetroStop("Policlinico", "", ""),
                MetroStop("Rione Alto", "", ""),
                MetroStop("Montedonzelli", "", ""),
                MetroStop("Medaglie d'Oro", "", ""),
                MetroStop("Vanvitelli", "Funicolare Centrale, Funicolare di Chiaia", ""),
                MetroStop("Quattro Giornate", "", ""),
                MetroStop("Salvator Rosa", "", ""),
                MetroStop("Materdei", "", ""),
                MetroStop("Museo", "Linea 2, Museo Archeologico", ""),
                MetroStop("Dante", "Linea 2 (a piedi)", ""),
                MetroStop("Toledo", "", "stazione più profonda d'Europa"),
                MetroStop("Municipio", "Linea 6, Molo Beverello", ""),
                MetroStop("Università", "", ""),
                MetroStop("Duomo", "", ""),
                MetroStop("Garibaldi", "Linea 2, FS, Circumvesuviana, Trenitalia", "capolinea sud"),
            ),
            frequency = "Ogni 8-12 min (giorno feriale), ogni 12-15 min (festivo)",
            firstLast = "05:50 – 23:30 (feriale), 06:00 – 23:30 (festivo)",
            info = "La metro L1 attraversa Napoli da nord (Piscinola) al centro (Garibaldi). Collegamento con AV, circumvesuviana, funicolari e linee 2-6-11.",
        ),
        MetroLine(
            name = "Linea 2",
            colorName = "Blu",
            colorHex = 0xFF1E90FF,
            operator = "Trenitalia",
            stops = listOf(
                MetroStop("Pozzuoli Solfatara", "", "capolinea ovest"),
                MetroStop("Bagnoli", "", ""),
                MetroStop("Cavalleggeri", "", ""),
                MetroStop("Campi Flegrei", "Cumana", ""),
                MetroStop("Piazza Leopardi", "", ""),
                MetroStop("Mergellina", "Funicolare di Mergellina, Linea 6", ""),
                MetroStop("Piazza Amedeo", "Funicolare di Chiaia", ""),
                MetroStop("Montesanto", "Funicolare di Montesanto, Cumana", ""),
                MetroStop("Piazza Cavour", "Museo (Linea 1)", ""),
                MetroStop("Garibaldi", "Linea 1, FS, Circumvesuviana, Trenitalia AV", ""),
                MetroStop("Gianturco", "", ""),
                MetroStop("San Giovanni-Barra", "", "capolinea est"),
            ),
            frequency = "Ogni 6-15 min",
            firstLast = "05:30 – 23:00",
            info = "Linea metropolitana/ferroviaria gestita da Trenitalia. Collega Pozzuoli a San Giovanni passando per il centro di Napoli.",
        ),
        MetroLine(
            name = "Linea 6",
            colorName = "Viola",
            colorHex = 0xFF8B00FF,
            operator = "ANM",
            stops = listOf(
                MetroStop("Mergellina", "Linea 2, Funicolare di Mergellina", "capolinea ovest"),
                MetroStop("Arco Mirelli", "", ""),
                MetroStop("San Pasquale a Chiaia", "", ""),
                MetroStop("Chiaia", "", ""),
                MetroStop("Municipio", "Linea 1, Molo Beverello", "capolinea"),
            ),
            frequency = "Ogni 10-15 min",
            firstLast = "07:00 – 21:00",
            info = "Linea metropolitana leggera che collega il quartiere Chiaia con la stazione di Mergellina e il centro (Municipio).",
        ),
        MetroLine(
            name = "Linea 11",
            colorName = "Arancione",
            colorHex = 0xFFFF8C00,
            operator = "EAV",
            stops = listOf(
                MetroStop("Piscinola", "Linea 1", "capolinea sud"),
                MetroStop("Mugnano", "", ""),
                MetroStop("Melito", "", ""),
                MetroStop("Giugliano", "", ""),
                MetroStop("Aversa Ippodromo", "", ""),
                MetroStop("Aversa Centro", "", "capolinea nord"),
            ),
            frequency = "Ogni 15-30 min",
            firstLast = "06:00 – 22:00",
            info = "Linea suburbana EAV che collega Piscinola (Metro L1) con l'hinterland nord (Aversa).",
        ),
        MetroLine(
            name = "Funicolare Centrale",
            colorName = "Rosso",
            colorHex = 0xFFFF4500,
            operator = "ANM",
            stops = listOf(
                MetroStop("Piazzetta Augusteo", "Piazza Trieste e Trento, Toledo (L1)", "capolinea basso"),
                MetroStop("Piazza Fuga", "Vanvitelli (L1)", "capolinea alto"),
            ),
            frequency = "Ogni 3-8 min",
            firstLast = "06:30 – 01:00",
            info = "Collega il centro (Piazza Trieste e Trento) con il Vomero (Piazza Fuga/Vanvitelli).",
        ),
        MetroLine(
            name = "Funicolare di Chiaia",
            colorName = "Verde",
            colorHex = 0xFF32CD32,
            operator = "ANM",
            stops = listOf(
                MetroStop("Parco Margherita", "Chiaia", "capolinea basso"),
                MetroStop("Cimarosa", "Piazza Amedeo (L2)", ""),
                MetroStop("Vanvitelli", "Linea 1, Funicolare Centrale", "capolinea alto"),
            ),
            frequency = "Ogni 5-10 min",
            firstLast = "07:00 – 22:00",
            info = "Collega il quartiere Chiaia con il Vomero (Vanvitelli).",
        ),
        MetroLine(
            name = "Funicolare di Montesanto",
            colorName = "Marrone",
            colorHex = 0xFF8B4513,
            operator = "ANM",
            stops = listOf(
                MetroStop("Montesanto", "Linea 2, Cumana", "capolinea basso"),
                MetroStop("Morghen", "Vanvitelli (L1) — a piedi 5 min", "capolinea alto"),
            ),
            frequency = "Ogni 5-10 min",
            firstLast = "06:30 – 22:30",
            info = "Collega la stazione di Montesanto (L2/Cumana) con il Vomero.",
        ),
        MetroLine(
            name = "Funicolare di Mergellina",
            colorName = "Azzurro",
            colorHex = 0xFF00CED1,
            operator = "ANM",
            stops = listOf(
                MetroStop("Via Mergellina", "Mergellina (L2, L6)", "capolinea basso"),
                MetroStop("Via Manzoni", "", "capolinea alto"),
            ),
            frequency = "Ogni 8-15 min",
            firstLast = "07:00 – 21:30",
            info = "Collega la zona di Mergellina (Lungomare) con Via Manzoni (collina di Posillipo).",
        ),
    )

    suspend fun fetchStrikesCached(): List<StrikeInfo> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date())
        if (strikeCache != null && strikeCacheDate == today) {
            return strikeCache!!
        }
        val strikes = fetchStrikes()
        strikeCache = strikes
        strikeCacheDate = today
        return strikes
    }

    private suspend fun fetchStrikes(): List<StrikeInfo> = withContext(Dispatchers.IO) {
        try {
            val catJson = Jsoup.connect("https://www.eavsrl.it/wp-json/wp/v2/categories?slug=linee-vesuviane")
                .ignoreContentType(true).timeout(15000).execute().body()
            val catArr = JSONArray(catJson)
            if (catArr.length() == 0) return@withContext emptyList()
            val catId = catArr.getJSONObject(0).getInt("id")

            val postsJson = Jsoup.connect("https://www.eavsrl.it/wp-json/wp/v2/posts")
                .data("categories", catId.toString())
                .data("per_page", "10")
                .data("orderby", "date")
                .data("order", "desc")
                .ignoreContentType(true)
                .timeout(15000)
                .execute().body()

            val posts = JSONArray(postsJson)
            val result = mutableListOf<StrikeInfo>()
            for (i in 0 until posts.length()) {
                val post = posts.getJSONObject(i)
                val title = post.getJSONObject("title").getString("rendered")
                val link = post.getString("link")
                val rawHtml = post.getJSONObject("content").getString("rendered")
                val parsed = parseStrikeHtml(rawHtml)
                result.add(StrikeInfo(
                    title = title,
                    link = link,
                    announcement = parsed["announcement"] ?: "",
                    guaranteeHours = parsed["guaranteeHours"],
                    lastNapoliSorrento = parsed["lastNapoliSorrento"],
                    firstNapoliSorrento = parsed["firstNapoliSorrento"],
                    lastSorrentoNapoli = parsed["lastSorrentoNapoli"],
                    firstSorrentoNapoli = parsed["firstSorrentoNapoli"],
                    rawDate = post.getString("date"),
                ))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStrikeHtml(html: String): Map<String, String?> {
        val cleanHtml = Jsoup.clean(html, org.jsoup.safety.Safelist.relaxed())
        val doc = Jsoup.parse(cleanHtml)
        val text = doc.body().text()

        val announcement = doc.select("p").firstOrNull()?.text() ?: text.take(200)

        val guaranteeRegex = Regex("Servizio garantito[^.]*\\.", RegexOption.IGNORE_CASE)
        val guaranteeHours = guaranteeRegex.find(text)?.value

        val sorrentoTimeRegex = Regex("""[–\-]?\s*Sorrento\s*:?\s*(\d{1,2}:\d{2})""", RegexOption.IGNORE_CASE)

        // Find "da Napoli per" section and "per Napoli da" section
        val sections = text.split(Regex("(?=Ultime partenze|Prime partenze|Giorno \\d+)", RegexOption.IGNORE_CASE))
        var lastNapoliSorrento: String? = null
        var lastSorrentoNapoli: String? = null
        var firstNapoliSorrento: String? = null
        var firstSorrentoNapoli: String? = null

        val lastSection = sections.firstOrNull { it.contains("Ultime partenze", ignoreCase = true) }
        val firstSection = sections.firstOrNull { it.contains("Prime partenze", ignoreCase = true) }

        fun extractTimes(section: String): Pair<String?, String?> {
            val napoliPart = section.substringAfter("da Napoli per", "").substringBefore("per Napoli da")
            val sorrentoPart = section.substringAfter("per Napoli da", "").substringBefore("da Napoli")
            val fromNapoli = sorrentoTimeRegex.find(napoliPart)?.groupValues?.get(1)
            val fromSorrento = sorrentoTimeRegex.find(sorrentoPart)?.groupValues?.get(1)
            return fromNapoli to fromSorrento
        }

        if (lastSection != null) {
            val (nap, sor) = extractTimes(lastSection)
            lastNapoliSorrento = nap
            lastSorrentoNapoli = sor
        }
        if (firstSection != null) {
            val (nap, sor) = extractTimes(firstSection)
            firstNapoliSorrento = nap
            firstSorrentoNapoli = sor
        }

        // fallback: if no sections found, search the whole text
        if (lastNapoliSorrento == null && firstNapoliSorrento == null) {
            val fullNap = extractTimes(text)
            lastNapoliSorrento = fullNap.first
            lastSorrentoNapoli = fullNap.second
        }

        return mapOf(
            "announcement" to announcement,
            "guaranteeHours" to guaranteeHours,
            "lastNapoliSorrento" to lastNapoliSorrento,
            "lastSorrentoNapoli" to lastSorrentoNapoli,
            "firstNapoliSorrento" to firstNapoliSorrento,
            "firstSorrentoNapoli" to firstSorrentoNapoli,
        )
    }
}
