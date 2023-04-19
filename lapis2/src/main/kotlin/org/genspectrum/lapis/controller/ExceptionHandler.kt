package org.genspectrum.lapis.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.silo.SiloException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Throwable::class)
    fun handleUnexpectedException(e: Throwable): ResponseEntity<String> {
        log.error(e) { "Caught unexpected exception: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Unexpected error",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
        log.error(e) { "Caught IllegalArgumentException: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Bad request",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(SiloException::class)
    fun handleSiloException(e: SiloException): ResponseEntity<String> {
        log.error(e) { "Caught SiloException: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Silo error",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(SiloNotImplementedError::class)
    fun handleNotImplementedError(e: SiloNotImplementedError): ResponseEntity<String> {
        log.error(e) { "Caught SiloNotImplementedError: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Not implemented",
                        "${e.message}",
                    ),
                ),
            )
    }
}

data class LapisHttpErrorResponse(val title: String, val message: String)
