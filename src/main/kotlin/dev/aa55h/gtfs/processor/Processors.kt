package dev.aa55h.gtfs.processor

import dev.aa55h.gtfs.csv.GeocodeQuery
import dev.aa55h.gtfs.csv.StopsFile
import dev.aa55h.gtfs.csv.queryGeocode
import java.nio.file.Files
import java.nio.file.Path

class StripLinesProcessor(val toRemove: Set<String>) : Processor {
    override fun process(input: Path) {
        val linesToKeep = mutableListOf<String>()
        Files.lines(input).parallel().forEachOrdered {
            if (it !in toRemove) {
                linesToKeep.add(it)
            }
        }
        Files.write(input, linesToKeep)
    }
}

class FixStopCoordsProcessor(val apiKey: String, val cache: MutableMap<String, Pair<String, String>> = mutableMapOf()) : Processor {
    override fun process(input: Path) {
        if (input.fileName.toString() == "stops.txt") {
            val stops = StopsFile(input)
            var invalid = 0
            stops.stops = stops.stops.parallelStream().filter {
                if (it.stopLat == "0" || it.stopLon == "0") {
                    cache.getOrElse(it.stopName!!) {
                        val geocode = queryGeocode(apiKey, GeocodeQuery(it.stopName ?: "Undefined"))
                        if (geocode.items.isEmpty()) {
                            println("[FixStopCoordsProcessor] No coordinates found for stop: ${it.stopName}")
                            invalid++
                            return@filter false
                        } else {
                            val res = geocode.items.filter { res -> res.type === "poi" }
                                .getOrElse(0) { _ ->
                                    println("[FixStopCoordsProcessor][WARN] No POI found for stop: ${it.stopName}, using first result")
                                    return@getOrElse geocode.items[0]
                                }
                            it.stopLat = res.position.lat.toString()
                            it.stopLon = res.position.lon.toString()
                        }
                    }
                }
                return@filter true
            }.toList()
            println("[FixStopCoordsProcessor] Fixed $invalid stops with invalid coordinates in ${input.fileName}")
            stops.writeTo(input)
        }
    }
}