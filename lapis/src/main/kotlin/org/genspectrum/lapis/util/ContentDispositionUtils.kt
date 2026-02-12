package org.genspectrum.lapis.util

import org.springframework.http.ContentDisposition
import java.nio.charset.StandardCharsets

/**
 * Generates a Content-Disposition header value with dual filename parameters
 * for maximum browser compatibility.
 *
 * @param filename The original filename (may contain UTF-8 characters)
 * @return Content-Disposition header value in format:
 *         "attachment; filename=<ascii-only>; filename*=UTF-8''<percent-encoded>"
 *
 * Example: generateContentDisposition("données.json")
 *         → "attachment; filename=donnes.json; filename*=UTF-8''donn%C3%A9es.json"
 */
fun generateContentDisposition(filename: String): String {
    // Use Spring to generate RFC 5987 encoded filename*
    val springDisposition = ContentDisposition.attachment()
        .filename(filename, StandardCharsets.UTF_8)
        .build()
        .toString()

    // Extract filename* part from Spring's output using a robust regex
    val filenameStar = Regex("""filename\*=([^;]+)""")
        .find(springDisposition)
        ?.groupValues
        ?.get(1)
        ?: throw IllegalStateException(
            "Spring ContentDisposition does not contain a valid filename* parameter: $springDisposition",
        )

    // Filter to ASCII-only for plain filename parameter
    val asciiFilename = toAsciiFilename(filename)

    return "attachment; filename=$asciiFilename; filename*=$filenameStar"
}

/**
 * Filters a filename to contain only ASCII characters (code < 128).
 * Non-ASCII characters are removed entirely.
 *
 * @param filename The filename to filter
 * @return ASCII-only filename
 *
 * Examples:
 * - "aggregated.json" → "aggregated.json" (unchanged)
 * - "données.json" → "donnes.json" (é removed)
 * - "测试.json" → ".json" (Chinese characters removed)
 */
fun toAsciiFilename(filename: String): String =
    filename.filter {
        it.code in 32..126 && it !in setOf(';', '"', '\\')
    }
