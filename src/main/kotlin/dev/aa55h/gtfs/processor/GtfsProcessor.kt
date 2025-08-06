package dev.aa55h.gtfs.processor

import dev.aa55h.gtfs.extract
import dev.aa55h.gtfs.packageToZip
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

data class GtfsProcessor(
    val input: Path,
    var output: Path,
    val processors: MutableSet<Processor> = mutableSetOf(),
) {
    fun processor(processor: Processor): GtfsProcessor {
        processors.add(processor)
        return this
    }
    
    @OptIn(ExperimentalPathApi::class)
    fun execute() {
        val inputting = Files.createTempDirectory("gtfs-processor-in")
        println("[GtfsProcessor] Processors(${processors.size}): ${processors.joinToString(", ") { it::class.simpleName ?: "Unknown" }}")
        println("[GtfsProcessor] Extracting GTFS data from ${input.toAbsolutePath()} to $inputting")
        ZipFile(input.toFile()).use { zip ->
            zip.extract(inputting)
        }
        println("[GtfsProcessor] Processing GTFS data in $inputting")
        Files.list(inputting).parallel().forEach { file ->
            processors.forEach {
                it.process(file)
            }
        }
        println("[GtfsProcessor] Zipping processed GTFS data to ${output.toAbsolutePath()}")
        inputting.packageToZip(output)
        inputting.deleteRecursively()
        println("[GtfsProcessor] Processed GTFS data saved to ${output.toAbsolutePath()}")
    }
}

fun interface Processor {
    fun process(input: Path)
}

fun main() {
    GtfsProcessor(
        input = Path.of("input.zip"),
        output = Path.of("output.zip"),
    )
        .processor(FixStopCoordsProcessor("YOUR_API_KEY_HERE"))
        .execute()
}