package org.genspectrum.lapis.request

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.LapisErrorResponse
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisResponse
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_EXAMPLE
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION
import org.genspectrum.lapis.openApi.LAPIS_INFO_DESCRIPTION
import org.genspectrum.lapis.openApi.REQUEST_ID_HEADER_DESCRIPTION
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

private const val REPORT_TO =
    "Please report to https://github.com/GenSpectrum/LAPIS/issues in case you encounter any unexpected issues. " +
        "Please include the request ID and the requestInfo in your report."

@Schema(description = LAPIS_INFO_DESCRIPTION)
data class LapisInfo(
    @Schema(
        description = LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION,
        example = LAPIS_DATA_VERSION_EXAMPLE,
    )
    var dataVersion: String? = null,
    @Schema(
        description = REQUEST_ID_HEADER_DESCRIPTION,
        example = "dfb342ea-3607-4caf-b35e-9aba75d06f81",
    )
    var requestId: String? = null,
    @Schema(
        description = "Some information about the request in human readable form. Intended for debugging.",
        example = "my_instance on my.server.com at 2024-01-01T12:00:00.0000",
    )
    var requestInfo: String? = null,
    @Schema(example = REPORT_TO)
    val reportTo: String = REPORT_TO,
)

@ControllerAdvice
class ResponseBodyAdviceDataVersion(
    private val dataVersion: DataVersion,
    private val requestIdContext: RequestIdContext,
    private val databaseConfig: DatabaseConfig,
) : ResponseBodyAdvice<Any> {
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        response.headers.add(LAPIS_DATA_VERSION, dataVersion.dataVersion)

        val isDownload = response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)?.startsWith("attachment") ?: false

        request.uri.host

        return when {
            body is LapisResponse<*> && isDownload -> body.data
            body is LapisResponse<*> -> LapisResponse(body.data, getLapisInfo(request))
            body is LapisErrorResponse -> LapisErrorResponse(body.error, getLapisInfo(request))
            else -> body
        }
    }

    private fun getLapisInfo(request: ServerHttpRequest) =
        LapisInfo(
            dataVersion = dataVersion.dataVersion,
            requestId = requestIdContext.requestId,
            requestInfo = "${databaseConfig.schema.instanceName} on ${request.uri.host} at ${now()}",
        )

    private fun now(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
    }

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean {
        return true
    }
}
