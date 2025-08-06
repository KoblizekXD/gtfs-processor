package dev.aa55h.gtfs

import dev.aa55h.gtfs.lines.fetchLines
import dev.aa55h.gtfs.lines.transportOptions
import dev.aa55h.gtfs.processor.FixStopCoordsProcessor
import dev.aa55h.gtfs.processor.GtfsProcessor
import dev.aa55h.gtfs.processor.StripLinesProcessor
import java.nio.file.Path
import kotlin.io.path.Path

// Usage: java -jar gtfs-processor.jar input.zip output.zip coordCache.txt apiKey
fun main(args: Array<String>) {
    val lines = fetchLines(
        listOf(
            "FlixBus CZ s.r.o.",
            "FlixBus DACH GmbH",
            "FlixBus Polska sp. z o.o.",
            "Plzeňské městské dopravní podniky, a.s.",
            "Dopravní podnik města Olomouce, a.s."
        ),
        listOf(
            transportOptions["Pražská integrovaná doprava"]!!,
            transportOptions["IDS Jihomoravského kraje"]!!,
            transportOptions["Integrovaná doprava Zlínského kraje"]!!,
        )
    ).map { it.line.toString() }
    val coordCache = loadCoordCache(Path(args[2]))
    GtfsProcessor(Path(args[0]), Path(args[1]))
        .processor(StripLinesProcessor(lines.toSet()))
        .processor(FixStopCoordsProcessor(args[3], coordCache))
        .execute()
    saveCoordCache(Path(args[2]), coordCache)
}

fun loadCoordCache(path: Path): MutableMap<String, Pair<String, String>> {
    val cache = mutableMapOf<String, Pair<String, String>>()
    if (path.toFile().exists()) {
        path.toFile().readLines().stream().skip(1).forEach { line ->
            val parts = line.split("\t")
            if (parts.size == 3) {
                cache[parts[0]] = Pair(parts[1], parts[2])
            }
        }
    }
    return cache
}

fun saveCoordCache(path: Path, cache: Map<String, Pair<String, String>>) {
    path.toFile().bufferedWriter().use { writer ->
        writer.write("stop\tlatitude\tlongtitude\n")
        cache.forEach { (key, value) ->
            writer.write("$key\t${value.first}\t${value.second}\n")
        }
    }
}