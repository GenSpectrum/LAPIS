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

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpectedException(e: Throwable): ResponseEntity<LapisHttpErrorResponse> {
        log.error(e) { "Caught unexpected exception: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisHttpErrorResponse(
                    "Unexpected error",
                    "${e.message}",
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<LapisHttpErrorResponse> {
        log.error(e) { "Caught IllegalArgumentException: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisHttpErrorResponse(
                    "Bad request",
                    "${e.message}",
                ),
            )
    }

    @ExceptionHandler(AddForbiddenToOpenApiDocsHelper::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(e: AddForbiddenToOpenApiDocsHelper): ResponseEntity<LapisHttpErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisHttpErrorResponse(
                    "Forbidden",
                    "${e.message}",
                ),
            )
    }

    @ExceptionHandler(SiloException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleSiloException(e: SiloException): ResponseEntity<LapisHttpErrorResponse> {
        log.error(e) { "Caught SiloException: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisHttpErrorResponse(
                    "Silo error",
                    "${e.message}",
                ),
            )
    }

    @ExceptionHandler(SiloNotImplementedError::class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    fun handleNotImplementedError(e: SiloNotImplementedError): ResponseEntity<LapisHttpErrorResponse> {
        log.error(e) { "Caught SiloNotImplementedError: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisHttpErrorResponse(
                    "Not implemented",
                    "${e.message}",
                ),
            )
    }
}

/** This is not yet actually thrown, but makes "403 Forbidden" appear in OpenAPI docs. */
class AddForbiddenToOpenApiDocsHelper(message: String) : Exception(message)

data class LapisHttpErrorResponse(val title: String, val message: String)
