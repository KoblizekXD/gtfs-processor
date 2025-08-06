package dev.aa55h.gtfs.csv

import com.fasterxml.jackson.module.kotlin.readValue
import dev.aa55h.gtfs.objectMapper
import dev.aa55h.gtfs.okHttpClient
import okhttp3.Request

const val baseDomain = "https://api.mapy.com"

data class GeocodeQuery(
    val query: String,
    val lang: String = "cs",
    val limit: Int = 1,
    val locality: String = "cz"
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
    val url = "$baseDomain/geocoding/v1/geocode?query=${queryParams.query}&lang=${queryParams.lang}&limit=${queryParams.limit}&locality=${queryParams.locality}"
    val request = Request.Builder()
        .get()
        .url(url)
        .header("Authorization", "Bearer $apiKey").build()
    return okHttpClient.newCall(request).execute().use { response ->
        objectMapper.readValue<GeocodeResponse>(response.body.string())
    }
}