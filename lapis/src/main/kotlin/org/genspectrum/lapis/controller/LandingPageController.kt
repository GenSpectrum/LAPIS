package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Hidden
import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class LandingPageController(
    @Value("\${lapis.docs.url:}") lapisDocsUrl: String,
    private val databaseConfig: DatabaseConfig,
) {
    private val links = buildMap {
        if (lapisDocsUrl.isNotBlank()) {
            put("Documentation", lapisDocsUrl)
        }
        put("Swagger UI", "swagger-ui/index.html")
        put("OpenAPI specification JSON", "api-docs")
        put("OpenAPI specification YAML", "api-docs.yaml")
        put("GitHub", "https://github.com/GenSpectrum/LAPIS")
    }

    @RequestMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun helloHtml() =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>LAPIS - ${databaseConfig.schema.instanceName}</title>
        </head>
        <body>
            <h1>LAPIS - ${databaseConfig.schema.instanceName}</h1>
            Welcome to the LAPIS instance for ${databaseConfig.schema.instanceName}.
            You can find more information on the following pages:
            <ul>
                ${getHtmlLinkItems()}
            </ul>
        </body>
        </html>
        """.trimIndent()

    @RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun helloJson() = links + ("Instance name" to databaseConfig.schema.instanceName)

    private fun getHtmlLinkItems() =
        links
            .map { (title, url) -> """<li><a href="$url">$title</a></li>""" }
            .joinToString("")
}
