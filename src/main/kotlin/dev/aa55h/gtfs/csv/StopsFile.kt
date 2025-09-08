package dev.aa55h.gtfs.csv

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.writer

/**
 * Parse GTFS stops.txt file.
 * Optionally filter stops without GPS coordinates for easy MapyCZ API geocoding.
 */
data class StopsFile(val path: Path) {
    
    private val header: String
    
    var stops: List<Stop> = CSVReader(path.toFile().bufferedReader()).use {
        header = it.readNext()?.joinToString(",") ?: ""
        it.readAll().map { row ->
            Stop.fromArray(row)
        }
    }
    
    val noGpsStops: List<Stop> by lazy {
        stops.filter { 
            it.hasInvalidCoords()
        }
    }
    
    fun getValid(stopName: String): Stop = stops.first { it.stopName == stopName && !it.hasInvalidCoords() }
    
    fun writeTo(output: Path) {
        CSVWriter(output.writer()).use { 
            it.writeNext(header.split(",").toTypedArray())
            stops.forEach { stop ->
                it.writeNext(
                    arrayOf(
                        stop.stopId ?: "",
                        stop.stopCode ?: "",
                        stop.stopName ?: "",
                        stop.stopDesc ?: "",
                        stop.stopLat ?: "",
                        stop.stopLon ?: "",
                        stop.zoneId ?: "",
                        stop.stopUrl ?: "",
                        stop.locationType ?: "",
                        stop.parentStation ?: "",
                        stop.stopTimezone ?: "",
                        stop.wheelchairBoarding ?: "",
                        stop.platformCode ?: ""
                    )
                )
            }
        }
    }
}