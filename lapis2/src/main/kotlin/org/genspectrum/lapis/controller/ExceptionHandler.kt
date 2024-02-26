package org.genspectrum.lapis.controller

import mu.KotlinLogging
import org.genspectrum.lapis.model.SiloNotImplementedError
import org.genspectrum.lapis.silo.SiloException
import org.genspectrum.lapis.silo.SiloUnavailableException
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.BodyBuilder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

private val log = KotlinLogging.logger {}

private typealias ErrorResponse = ResponseEntity<LapisErrorResponse>

/**
 * Taken from https://github.com/spring-projects/spring-framework/issues/31569#issuecomment-1825444419
 * Due to https://github.com/spring-projects/spring-framework/commit/c00508d6cf2408d06a0447ed193ad96466d0d7b4
 *
 * This forwards "404" errors to the ErrorController to allow it to return a view.
 * Thus, browsers get their own error page.
 *
 * Spring reworked handling of "404 not found" errors. This was introduced with the upgrade to Spring boot 3.2.0.
 * This can be removed/reworked once Spring decides on how to return a view from an ExceptionHandler
 * or allows them to respect the Accept header.
 */
@ControllerAdvice
@Order(-1)
internal class ExceptionToErrorControllerBypass {
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleResourceNotFound(e: Exception): Nothing {
        throw e
    }
}

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpectedException(e: Throwable): ErrorResponse {
        log.warn(e) { "Caught unexpected exception: ${e.message}" }

        return responseEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
    }

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequestException(e: BadRequestException): ErrorResponse {
        log.warn(e) { "Caught BadRequestException: ${e.message}" }

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

    @ExceptionHandler(SiloUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleSiloUnavailableException(e: SiloUnavailableException): ErrorResponse {
        log.warn(e) { "Caught SiloUnavailableException: ${e.message}" }

        return responseEntity(HttpStatus.SERVICE_UNAVAILABLE, e.message) { header("Retry-After", e.retryAfter) }
    }

    private fun responseEntity(
        httpStatus: HttpStatus,
        detail: String?,
        alsoDoOnBuilder: BodyBuilder.() -> Unit = {},
    ) = responseEntity(httpStatus, httpStatus.reasonPhrase, detail, alsoDoOnBuilder)

    private fun responseEntity(
        httpStatus: HttpStatusCode,
        title: String,
        detail: String?,
        alsoDoOnBuilder: BodyBuilder.() -> Unit = {},
    ) = responseEntity(httpStatus.value(), title, detail, alsoDoOnBuilder)

    private fun responseEntity(
        httpStatus: Int,
        title: String,
        detail: String?,
        alsoDoOnBuilder: BodyBuilder.() -> Unit = {},
    ): ErrorResponse {
        return ResponseEntity
            .status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .also(alsoDoOnBuilder)
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

class BadRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)
