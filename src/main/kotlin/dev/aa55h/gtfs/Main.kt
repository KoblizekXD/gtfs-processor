package dev.aa55h.gtfs

import dev.aa55h.gtfs.lines.fetchLines
import dev.aa55h.gtfs.lines.transportOptions
import dev.aa55h.gtfs.processor.FixStopCoordsProcessor
import dev.aa55h.gtfs.processor.GtfsProcessor
import dev.aa55h.gtfs.processor.StripLinesProcessor
import dev.aa55h.gtfs.processor.ZollMakeUnboardableProcessor
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
    val startingWith = setOf(
        "199",
        "205",
        "289",
        "737",
        "755",
        "805",
        "825",
        "826",
        "835"
    )
    val coordCache = loadCoordCache(Path(args[2]))
    GtfsProcessor(Path(args[0]), Path(args[1]))
        .processor(StripLinesProcessor(lines.toSet(), startingWith))
        .processor(FixStopCoordsProcessor(args[3], coordCache, strategy = FixStopCoordsProcessor.Strategy.REMOVE))
        .processor(ZollMakeUnboardableProcessor())
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
        writer.write("stop\tlatitude\tlongitude\n")
        cache.forEach { (key, value) ->
            writer.write("$key\t${value.first}\t${value.second}\n")
        }
    }
}