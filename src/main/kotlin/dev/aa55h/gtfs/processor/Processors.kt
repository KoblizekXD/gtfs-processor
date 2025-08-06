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
                    val resulting = cache.getOrElse(it.stopName!!) {
                        val geocode = queryGeocode(apiKey, GeocodeQuery(it.stopName ?: "Undefined"))
                        if (geocode.items.isEmpty()) {
                            invalid++
                            return@filter false
                        } else {
                            val res = geocode.items.find { it.type == "poi" }
                                /*.getOrElse(0) { _ ->
                                    println("[FixStopCoordsProcessor][WARN] No POI found for stop: ${it.stopName}, using first result")
                                    invalid++
                                    return@getOrElse geocode.items[0]
                                }*/
                            if (res == null) {
                                invalid++
                                return@filter false
                            }
                            it.stopLat = res.position.lat.toString()
                            it.stopLon = res.position.lon.toString()
                            cache[it.stopName!!] = it.stopLat!! to it.stopLon!!
                            println("[FixStopCoordsProcessor] Fixed stop: ${it.stopName} to coords: ${it.stopLat}, ${it.stopLon}")
                            return@getOrElse it.stopLat!! to it.stopLon!!
                        }
                    }
                    it.stopLat = resulting.first
                    it.stopLon = resulting.second
                }
                return@filter true
            }.toList()
            println("[FixStopCoordsProcessor] Could not fix $invalid out of ${stops.noGpsStops.size} stop coordinates in ${input.fileName}")
            stops.writeTo(input)
        }
    }
}