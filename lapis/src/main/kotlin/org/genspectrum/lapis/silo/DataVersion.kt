package org.genspectrum.lapis.silo

import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
data class DataVersion(
    var dataVersion: String? = null,
)

fun DataVersion.setHeaderOn(response: HttpServletResponse) {
    dataVersion?.let { response.setHeader(LAPIS_DATA_VERSION, it) }
}

fun DataVersion.setHeaderOn(builder: ResponseEntity.BodyBuilder): ResponseEntity.BodyBuilder =
    dataVersion?.let { builder.header(LAPIS_DATA_VERSION, it) } ?: builder
