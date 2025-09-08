package dev.aa55h.gtfs.csv

import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

fun main(args: Array<String>) {
    if (args.size < 3) {
        println("Usage: CacheGen <apiKey> <inputFile> <outputFile>")
        return
    }
    val apiKey = args[0]
    val inputFile = Path(args[1])
    val outputFile = Path(args[2])
    
    
    val stopsFile = StopsFile(inputFile)
    outputFile.bufferedWriter().use { writer ->
        writer.write("stop\tlatitude\tlongitude\n")
        val nameCounts = stopsFile.stops.groupingBy { it.stopName }.eachCount()
        val uniqueNoGpsStops = stopsFile.noGpsStops.filter { nameCounts[it.stopName] == 1 }
        uniqueNoGpsStops.forEach { stop ->
            val geocode = queryGeocode(apiKey, GeocodeQuery("zastávka ${stop.stopName}"))
            val found = geocode.items.find { it.type == "poi" }
            if (found != null) {
                writer.write("${stop.stopName}\t${found.position.lat}\t${found.position.lon}\n")
            } else println("Could not resolve stop: ${stop.stopName}")
        }
    }
}