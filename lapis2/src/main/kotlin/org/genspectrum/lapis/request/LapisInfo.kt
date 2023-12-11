package org.genspectrum.lapis.request

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.LapisErrorResponse
import org.genspectrum.lapis.controller.LapisResponse
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_EXAMPLE
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION
import org.genspectrum.lapis.openApi.LAPIS_INFO_DESCRIPTION
import org.genspectrum.lapis.silo.DataVersion
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@Schema(description = LAPIS_INFO_DESCRIPTION)
data class LapisInfo(
    @Schema(
        description = LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION,
        example = LAPIS_DATA_VERSION_EXAMPLE,
    ) var dataVersion: String? = null,
)

const val LAPIS_DATA_VERSION_HEADER = "Lapis-Data-Version"

@ControllerAdvice
class ResponseBodyAdviceDataVersion(private val dataVersion: DataVersion) : ResponseBodyAdvice<Any> {
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        response.headers.add(LAPIS_DATA_VERSION_HEADER, dataVersion.dataVersion)

        when (body) {
            is LapisResponse<*> -> return LapisResponse(body.data, LapisInfo(dataVersion.dataVersion))
            is LapisErrorResponse -> return LapisErrorResponse(body.error, LapisInfo(dataVersion.dataVersion))
        }

        return body
    }

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean {
        return true
    }
}
