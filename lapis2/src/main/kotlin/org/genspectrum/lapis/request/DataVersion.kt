package org.genspectrum.lapis.request

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@Component
@RequestScope
class DataVersion {
    var dataVersion: String? = null
}

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
        response.headers.add("Lapis-Data-Version", dataVersion.dataVersion)
        return body
    }

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }
}
