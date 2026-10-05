package org.genspectrum.lapis.silo

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class RhyDbUris(
    @param:Value("\${silo.url}") private val rhyDbUrl: String,
) {
    val query = URI("$rhyDbUrl/query")
    val info = URI("$rhyDbUrl/info")

    fun lineageDefinition(column: String): URI = URI("$rhyDbUrl/lineageDefinition/").resolve(column)
}
