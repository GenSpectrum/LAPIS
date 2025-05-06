package org.genspectrum.lapis.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.response.LapisErrorResponse
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.silo.SiloException
import org.genspectrum.lapis.silo.SiloUnavailableException
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.BodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

private val log = KotlinLogging.logger {}

private typealias ErrorResponse = ResponseEntity<LapisErrorResponse>

@ControllerAdvice
class ExceptionHandler(
    private val notFoundView: NotFoundView,
    private val lapisInfoFactory: LapisInfoFactory,
) : ResponseEntityExceptionHandler() {
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
    fun handleForbiddenException(e: AddForbiddenToOpenApiDocsHelper): ErrorResponse =
        responseEntity(HttpStatus.FORBIDDEN, e.message)

    @ExceptionHandler(SiloException::class)
    fun handleSiloException(e: SiloException): ErrorResponse {
        log.warn(e) { "Caught SiloException: ${e.statusCode} - ${e.message}" }

        return responseEntity(e.statusCode, e.title, e.message)
    }

    @ExceptionHandler(SiloUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleSiloUnavailableException(e: SiloUnavailableException): ErrorResponse {
        log.warn(e) { "Caught SiloUnavailableException: ${e.message}" }

        return responseEntity(HttpStatus.SERVICE_UNAVAILABLE, e.message) { header(RETRY_AFTER, e.retryAfter) }
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val acceptMediaTypes = MediaType.parseMediaTypes(request.getHeaderValues(ACCEPT)?.toList())
        if (MediaType.TEXT_HTML.isPresentIn(acceptMediaTypes)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML)
                .body(notFoundView.render(request))
        }

        return super.handleNoResourceFoundException(ex, headers, status, request)
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
    ): ErrorResponse =
        ResponseEntity
            .status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .also(alsoDoOnBuilder)
            .body(
                LapisErrorResponse(
                    error = ProblemDetail.forStatus(httpStatus).also {
                        it.title = title
                        it.detail = detail
                    },
                    info = lapisInfoFactory.create(),
                ),
            )
}

@Component
class NotFoundView(
    private val databaseConfig: DatabaseConfig,
) {
    fun render(request: WebRequest): String {
        request.contextPath

        val helloUriBuilder = ServletUriComponentsBuilder.fromCurrentRequest()
            .replacePath("/${request.contextPath}")
            .replaceQuery(null)
            .fragment(null)
        val helloUri = helloUriBuilder.toUriString()

        val swaggerUri = helloUriBuilder
            .replacePath("/${request.contextPath}/swagger-ui/index.html")
            .toUriString()

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>LAPIS - Error 404</title>
            </head>
            <body>
                <h1>LAPIS - ${databaseConfig.schema.instanceName}</h1>
                <h3>Page not found!</h3>
                <div>
                    <a href="$helloUri">Back to landing page</a>
                </div>
                <div>
                    <a href="$swaggerUri">Visit our swagger-ui</a>
                </div>
            </body>
            </html>
            """.trimIndent()
    }
}

@Component
class ErrorController(
    private val databaseConfig: DatabaseConfig,
    errorAttributes: ErrorAttributes,
    serverProperties: ServerProperties,
) : BasicErrorController(errorAttributes, serverProperties.error) {
    override fun errorHtml(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ModelAndView {
        val modelAndView = super.errorHtml(request, response)

        val attributes = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL))
        modelAndView.view = View { _, _, viewResponse ->
            val html = """
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>LAPIS - Error</title>
                </head>
                <body>
                    <h1>LAPIS - ${databaseConfig.schema.instanceName}</h1>
                    <p>An error occurred: ${attributes["error"]}</p>
                    <p>Status code: ${attributes["status"]}</p>
                    <p>Error message: ${attributes["message"]}</p>
                </body>
                </html>
            """.trimIndent()
            viewResponse.outputStream.write(html.toByteArray())
        }
        return modelAndView
    }
}

/** This is not yet actually thrown, but makes "403 Forbidden" appear in OpenAPI docs. */
class AddForbiddenToOpenApiDocsHelper(
    message: String,
) : Exception(message)

class BadRequestException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
