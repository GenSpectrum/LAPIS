package org.genspectrum.lapis.util

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders

class ResponseWithContentType(
    private val response: HttpServletResponse,
    private val contentType: String?,
) : HttpServletResponse by response {
    init {
        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
    }

    override fun getHeaders(name: String?): MutableCollection<String> {
        if (name == HttpHeaders.CONTENT_TYPE && contentType != null) {
            return mutableListOf(contentType)
        }

        return response.getHeaders(name)
    }

    override fun getHeader(name: String): String? {
        if (name == HttpHeaders.CONTENT_TYPE && contentType != null) {
            return contentType
        }

        return response.getHeader(name)
    }
}
