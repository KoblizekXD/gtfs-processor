package dev.aa55h.gtfs.processor

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import dev.aa55h.gtfs.csv.GeocodeQuery
import dev.aa55h.gtfs.csv.Stop
import dev.aa55h.gtfs.csv.StopsFile
import dev.aa55h.gtfs.csv.queryGeocode
import org.ahocorasick.trie.Trie
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.bufferedWriter
import kotlin.io.path.reader
import kotlin.io.path.writer

class StripLinesProcessor(val toRemove: Set<String>, val startingWith: Set<String> = setOf()) : Processor {
    val routeIds = mutableSetOf<String>()
    lateinit var trie: Trie
    
    override fun requires(input: Path) {
        if (input.fileName.toString() != "routes.txt") return

        CSVReader(input.reader()).use { csvReader ->
            val header = csvReader.readNext() ?: return
            require(header.contains("route_long_name")) { "Input file does not contain route_long_name column" }
            
            csvReader.readAll().filter { toRemove.any { rem -> it[2] == rem } 
                    || startingWith.any { rem -> it[2].startsWith(rem) } }.forEach {
                routeIds.add(it[0])
            }
        }
        
        println("[StripLinesProcessor] Needs to remove ${routeIds.size} routes from ${input.parent}")
        trie = Trie.builder().addKeywords(routeIds).build()
    }
    
    override fun process(input: Path) {
        val tmp = Files.createTempFile("temp", ".txt")
        Files.lines(input).use { stream ->
            Files.newBufferedWriter(tmp).use { writer ->
                stream.filter { line -> trie.parseText(line).isEmpty() }
                    .forEach { writer.write(it + System.lineSeparator()) }
            }
        }
        Files.move(tmp, input, REPLACE_EXISTING)
    }
}

class ZollMakeUnboardableProcessor(): Processor {
    
    val toMarkAsUnboardable: MutableSet<String> = mutableSetOf()
    lateinit var header: String
    
    override fun requires(input: Path) {
        if (input.fileName.toString() != "stops.txt") return
        toMarkAsUnboardable.clear()
        val stops = StopsFile(input)
        stops.stops.filter { it.stopName?.contains("ZOLL") == true || it.stopName?.contains("CLO") == true }
            .forEach { toMarkAsUnboardable.add(it.stopId!!) }
        println("[ZollMakeUnboardableProcessor] Needs to mark ${toMarkAsUnboardable.size} stops as unboardable")
    }

    override fun process(input: Path) {
        val inputFile = input.toFile()
        val tempFile = Files.createTempFile("processed-", ".csv")

        CSVReader(inputFile.bufferedReader()).use { reader ->
            CSVWriter(tempFile.bufferedWriter()).use { writer ->
                val headerRow = reader.readNext()
                writer.writeNext(headerRow)

                var row: Array<String>? = reader.readNext()
                while (row != null) {
                    if (row[3] in toMarkAsUnboardable) {
                        row[6] = "1"
                        row[7] = "1"
                        println("[ZollMakeUnboardableProcessor] Marking stop ${row[3]} as unboardable")
                    }
                    writer.writeNext(row)
                    row = reader.readNext()
                }
            }
        }
        
        Files.move(tempFile, input, REPLACE_EXISTING)
    }

    override fun skip(input: Path): Boolean {
        return input.fileName.toString() != "stop_times.txt"
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
                val geocode = queryGeocode(apiKey, GeocodeQuery("zastávka ${stop.stopName!!}"))
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

    override fun skip(input: Path): Boolean {
        return input.fileName.toString() != "stops.txt"
    }

    enum class Strategy {
        FIX, // Attempt to retrieve coordinates from cache, if not found, query geocode API
        SKIP, // Attempt to retrieve coordinates from cache, if not found, skip the stop(keeping 0,0 coordinates)
        REMOVE // Attempt to retrieve coordinates from cache, if not found, remove the stop from the file
    }
}