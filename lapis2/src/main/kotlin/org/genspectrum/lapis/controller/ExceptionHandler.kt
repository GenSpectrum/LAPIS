package org.genspectrum.lapis.controller

import mu.KotlinLogging
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.silo.SiloException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

private typealias ErrorResponse = ResponseEntity<LapisErrorResponse>

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpectedException(e: Throwable): ErrorResponse {
        log.error(e) { "Caught unexpected exception: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    LapisError(
                        "Unexpected error",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ErrorResponse {
        log.error(e) { "Caught IllegalArgumentException: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    LapisError(
                        "Bad request",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(AddForbiddenToOpenApiDocsHelper::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(e: AddForbiddenToOpenApiDocsHelper): ErrorResponse {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    LapisError(
                        "Forbidden",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(SiloException::class)
    fun handleSiloException(e: SiloException): ErrorResponse {
        log.error(e) { "Caught SiloException: ${e.statusCode} - ${e.message}" }

        return ResponseEntity
            .status(e.statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    LapisError(
                        e.title,
                        e.message,
                    ),
                ),
            )
    }

    @ExceptionHandler(SiloNotImplementedError::class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    fun handleNotImplementedError(e: SiloNotImplementedError): ErrorResponse {
        log.error(e) { "Caught SiloNotImplementedError: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    LapisError(
                        "Not implemented",
                        "${e.message}",
                    ),
                ),
            )
    }
}

/** This is not yet actually thrown, but makes "403 Forbidden" appear in OpenAPI docs. */
class AddForbiddenToOpenApiDocsHelper(message: String) : Exception(message)

data class LapisError(val title: String, val message: String)
