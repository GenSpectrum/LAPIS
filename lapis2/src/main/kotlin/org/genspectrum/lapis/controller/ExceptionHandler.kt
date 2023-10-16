package org.genspectrum.lapis.controller

import mu.KotlinLogging
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.silo.SiloException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
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
        log.warn(e) { "Caught unexpected exception: ${e.message}" }

        return responseEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ErrorResponse {
        log.warn(e) { "Caught IllegalArgumentException: ${e.message}" }

        return responseEntity(HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(AddForbiddenToOpenApiDocsHelper::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(e: AddForbiddenToOpenApiDocsHelper): ErrorResponse {
        return responseEntity(HttpStatus.FORBIDDEN, e.message)
    }

    @ExceptionHandler(SiloException::class)
    fun handleSiloException(e: SiloException): ErrorResponse {
        log.warn(e) { "Caught SiloException: ${e.statusCode} - ${e.message}" }

        return responseEntity(e.statusCode, e.title, e.message)
    }

    @ExceptionHandler(SiloNotImplementedError::class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    fun handleNotImplementedError(e: SiloNotImplementedError): ErrorResponse {
        log.warn(e) { "Caught SiloNotImplementedError: ${e.message}" }

        return responseEntity(HttpStatus.NOT_IMPLEMENTED, e.message)
    }

    private fun responseEntity(httpStatus: HttpStatus, detail: String?) =
        responseEntity(httpStatus, httpStatus.reasonPhrase, detail)

    private fun responseEntity(httpStatus: HttpStatusCode, title: String, detail: String?) =
        responseEntity(httpStatus.value(), title, detail)

    private fun responseEntity(
        httpStatus: Int,
        title: String,
        detail: String?,
    ): ErrorResponse {
        return ResponseEntity
            .status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                LapisErrorResponse(
                    ProblemDetail.forStatus(httpStatus).also {
                        it.title = title
                        it.detail = detail
                    },
                ),
            )
    }
}

/** This is not yet actually thrown, but makes "403 Forbidden" appear in OpenAPI docs. */
class AddForbiddenToOpenApiDocsHelper(message: String) : Exception(message)
