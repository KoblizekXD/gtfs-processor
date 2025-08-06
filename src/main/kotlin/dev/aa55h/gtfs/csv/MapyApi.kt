package dev.aa55h.gtfs.csv

import com.fasterxml.jackson.module.kotlin.readValue
import dev.aa55h.gtfs.objectMapper
import dev.aa55h.gtfs.okHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val baseDomain = "https://api.mapy.com"

data class GeocodeQuery(
    val query: String,
    val lang: String = "cs",
    val limit: Int = 5,
    val locality: String = "cz",
    val type: String = "poi"
)

data class GeocodeResultEntry(
    val name: String,
    val label: String,
    val position: Position,
    val type: String,
    val location: String
)

data class Position(
    val lon: Double,
    val lat: Double
)

data class GeocodeResponse(
    val items: List<GeocodeResultEntry>,
)

fun queryGeocode(apiKey: String, queryParams: GeocodeQuery): GeocodeResponse {
    val url = "$baseDomain/v1/geocode?query=${URLEncoder.encode(queryParams.query, StandardCharsets.UTF_8)}&lang=${queryParams.lang}&limit=${queryParams.limit}&locality=${queryParams.locality}&type=${queryParams.type}"
    val request = Request.Builder()
        .get()
        .url(url)
        .header("X-Mapy-Api-Key", apiKey).build()
    return okHttpClient.newCall(request).execute().use { response ->
        objectMapper.readValue<GeocodeResponse>(response.body.string())
    }
}