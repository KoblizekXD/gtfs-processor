package dev.aa55h.gtfs.csv

import com.opencsv.CSVReader
import java.nio.file.Path
import kotlin.io.path.copyTo

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
            it.stopLat.isNullOrBlank() || it.stopLon.isNullOrBlank()
                    || it.stopLat == "0" || it.stopLon == "0"
        }
    }
    
    fun writeTo(output: Path) {
        path.copyTo(output, overwrite = true)
    }
}