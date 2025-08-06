package dev.aa55h.gtfs

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.getValue
import kotlin.io.path.outputStream

val okHttpClient by lazy {
    OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

val objectMapper: ObjectMapper by lazy {
    jacksonObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
val mediaTypeFormUrlEncoded = "application/x-www-form-urlencoded".toMediaType()

fun encodeFormData(raw: String): String {
    return raw
        .lines().joinToString("&") { line ->
            val (key, value) = line.split("=", limit = 2)
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
}

fun List<Any>.prettyPrint() {
    println("[\n${this.joinToString("\n") { "\t$it" }}\n][$size]")
}

fun ZipFile.extract(output: Path) {
    entries().asSequence().forEach { entry ->
        if (!entry.isDirectory) {
            val outputFile = output.resolve(entry.name)
            outputFile.parent.toFile().mkdirs()
            getInputStream(entry).use { inputStream ->
                Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

fun Path.packageToZip(output: Path) {
    // Step 1: Collect all regular files as pairs of relative path + file bytes in parallel
    val filesData = Files.walk(this)
        .filter { Files.isRegularFile(it) }
        .parallel()
        .map { file ->
            val relativePath = this.relativize(file).toString().replace("\\", "/")
            val bytes = Files.readAllBytes(file)  // read file fully into memory
            relativePath to bytes
        }
        .toList()

    // Step 2: Write all collected files sequentially into ZIP
    Files.newOutputStream(output).use { fos ->
        ZipOutputStream(fos).use { zipOut ->
            for ((relativePath, bytes) in filesData) {
                zipOut.putNextEntry(ZipEntry(relativePath))
                zipOut.write(bytes)
                zipOut.closeEntry()
            }
        }
    }
}