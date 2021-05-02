package com.miya10kei.emoji

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration

fun main() {
  val client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofMillis(300L))
    .version(HttpClient.Version.HTTP_1_1)
    .build()
  val request = HttpRequest.newBuilder()
    .GET()
    .uri(URI.create("https://api.github.com/emojis"))
    .header("Accept", "application/vnd.github.v3+json")
    .build()

  client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenAccept { response ->
      val data = Json.Default.decodeFromString<Map<String, String>>(response.body())
        .mapKeys { ":${it.key}:" }
        .mapValues { CodePoint(it.value) }
        .map { (k, v) -> "${k.padEnd(40)}$v" }

      val outputPath = Path.of("./dist/emoji.list")
      Files.write(outputPath, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE)
      println("Generate emoji dictionary: $outputPath")
    }
    .join()
}

class CodePoint(private val values: IntArray) {
  constructor(url: String) : this(
    url.substringAfterLast("/")
      .substringBefore(".")
      .split("-")
      .filter { it.isHex() }
      .map { it.toIntAsHex() }
      .toIntArray()
  )

  companion object {
    private fun String.isHex(): Boolean =
      kotlin.runCatching {
        Integer.parseInt(this, 16)
      }.isSuccess

    private fun String.toIntAsHex(): Int =
      if (isHex()) {
        Integer.parseInt(this, 16)
      } else {
        throw NumberFormatException("Can't convert to Hex Integer: $this")
      }
  }

  override fun toString() = String(this.values, 0, this.values.size)
}
