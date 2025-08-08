package dev.aa55h.gtfs.processor

import dev.aa55h.gtfs.csv.GeocodeQuery
import dev.aa55h.gtfs.csv.Stop
import dev.aa55h.gtfs.csv.StopsFile
import dev.aa55h.gtfs.csv.queryGeocode
import java.nio.file.Files
import java.nio.file.Path

class StripLinesProcessor(val toRemove: Set<String>) : Processor {
    override fun process(input: Path) {
        val tmp = Files.createTempFile("temp", ".txt")
        Files.lines(input).use { stream ->
            Files.newBufferedWriter(tmp).use { writer ->
                stream.filter { line -> toRemove.none { line.contains(it) } }
                    .forEach { writer.write(it + System.lineSeparator()) }
            }
        }
        Files.move(tmp, input, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
}


class FixStopCoordsProcessor(
    val apiKey: String, 
    val cache: MutableMap<String, Pair<String, String>> = mutableMapOf(),
    val strategy: Strategy = Strategy.FIX,
    // When strategy is FIX, this strategy will be used to handle stops that could not be fixed, not used otherwise
    val fixStrategy: Strategy = Strategy.REMOVE
) : Processor {
    
    private var fixed = 0
    private val failed: MutableList<String> = mutableListOf()
    
    // Assumes cache has been already tried
    private fun processInvalidStop(stop: Stop, strategy: Strategy): Stop? {
        return when (strategy) {
            Strategy.FIX -> {
                val geocode = queryGeocode(apiKey, GeocodeQuery(stop.stopName!!))
                val found = geocode.items.find { it.type == "poi" }
                if (found == null) {
                    return processInvalidStop(stop, fixStrategy)
                }
                stop.stopLat = found.position.lat.toString()
                stop.stopLon = found.position.lon.toString()
                cache[stop.stopName!!] = stop.stopLat!! to stop.stopLon!!
                fixed++
                return stop
            }
            Strategy.SKIP -> {
                failed.add(stop.stopName ?: "Unknown Stop")
                stop
            }
            Strategy.REMOVE -> {
                failed.add(stop.stopName ?: "Unknown Stop")
                null
            }
        }
    }
    
    override fun process(input: Path) {
        failed.clear()
        fixed = 0
        if (input.fileName.toString() == "stops.txt") {
            val stops = StopsFile(input)
            stops.stops = stops.stops.map { stop ->
                return@map if (stop.hasInvalidCoords()) {
                    val cached = cache[stop.stopName]
                    if (cached != null) {
                        stop.stopLat = cached.first
                        stop.stopLon = cached.second
                        fixed++
                        stop
                    } else processInvalidStop(stop, strategy)
                } else stop
            }.filterNotNull()
            println("[FixStopCoordsProcessor] Fixed $fixed stops, failed to fix $failed stops")
            stops.writeTo(input)
        }
    }
    
    enum class Strategy {
        FIX, // Attempt to retrieve coordinates from cache, if not found, query geocode API
        SKIP, // Attempt to retrieve coordinates from cache, if not found, skip the stop(keeping 0,0 coordinates)
        REMOVE // Attempt to retrieve coordinates from cache, if not found, remove the stop from the file
    }
}