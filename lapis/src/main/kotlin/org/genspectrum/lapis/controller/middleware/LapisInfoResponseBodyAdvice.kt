package org.genspectrum.lapis.controller.middleware

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.logging.RequestIdContext
import org.genspectrum.lapis.response.LapisErrorResponse
import org.genspectrum.lapis.response.LapisInfo
import org.genspectrum.lapis.response.LapisResponse
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@ControllerAdvice
class LapisInfoResponseBodyAdvice(
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
        response.headers.set(LAPIS_DATA_VERSION, dataVersion.dataVersion)

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
