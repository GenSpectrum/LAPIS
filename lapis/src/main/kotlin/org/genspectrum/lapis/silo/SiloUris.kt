package org.genspectrum.lapis.silo

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class SiloUris(
    @param:Value("\${silo.url}") private val siloUrl: String,
) {
    val query = URI("$siloUrl/query")
    val info = URI("$siloUrl/info")

    fun lineageDefinition(column: String): URI = URI("$siloUrl/lineageDefinition/").resolve(column)
}
