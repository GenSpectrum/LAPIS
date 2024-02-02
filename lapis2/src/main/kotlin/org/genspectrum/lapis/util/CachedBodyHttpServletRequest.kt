package org.genspectrum.lapis.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import mu.KotlinLogging
import org.springframework.http.HttpMethod
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

private val log = KotlinLogging.logger {}

class CachedBodyHttpServletRequest(request: HttpServletRequest, val objectMapper: ObjectMapper) :
    HttpServletRequestWrapper(request) {
    private val cachedBody: ByteArray by lazy {
        val inputStream: InputStream = request.inputStream
        val byteArrayOutputStream = ByteArrayOutputStream()

        inputStream.copyTo(byteArrayOutputStream)
        byteArrayOutputStream.toByteArray()
    }

    private val parsedBody: Map<String, JsonNode> by lazy {
        try {
            objectMapper.readValue(inputStream)
        } catch (exception: Exception) {
            if (method != HttpMethod.GET.name()) {
                log.warn { "Failed to read from request body: ${exception.message}, contentLength $contentLength" }
                log.debug { exception.stackTraceToString() }
            } else {
                log.warn {
                    "Tried to read request body of GET request: ${exception.message}, contentLength $contentLength"
                }
            }
            emptyMap()
        }
    }

    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        return CachedBodyServletInputStream(ByteArrayInputStream(cachedBody))
    }

    private inner class CachedBodyServletInputStream(private val cachedInputStream: ByteArrayInputStream) :
        ServletInputStream() {
        override fun isFinished(): Boolean {
            return cachedInputStream.available() == 0
        }

        override fun isReady(): Boolean {
            return true
        }

        override fun setReadListener(listener: ReadListener) {
            throw UnsupportedOperationException("setReadListener is not supported")
        }

        @Throws(IOException::class)
        override fun read(): Int {
            return cachedInputStream.read()
        }
    }

    fun getProxyAwarePath(): String {
        val isOperatedBehindAProxy = !contextPath.isNullOrBlank()
        return when {
            isOperatedBehindAProxy -> servletPath
            else -> requestURI
        }
    }

    fun getStringField(fieldName: String): String? {
        if (method == HttpMethod.GET.name()) {
            return parameterMap[fieldName]?.firstOrNull()
        }

        val fieldValue = parsedBody[fieldName]
        if (fieldValue?.isTextual == true) {
            return fieldValue.asText()
        }
        return null
    }

    fun getBooleanField(fieldName: String): Boolean? {
        if (method == HttpMethod.GET.name()) {
            return parameterMap[fieldName]?.firstOrNull()?.toBooleanStrictOrNull()
        }

        val fieldValue = parsedBody[fieldName]
        if (fieldValue?.isBoolean == true) {
            return fieldValue.asBoolean()
        }
        return null
    }

    fun getStringArrayField(fieldName: String): List<String> {
        if (method == HttpMethod.GET.name()) {
            return parameterMap[fieldName]?.flatMap { it.split(',') } ?: emptyList()
        }

        val fieldValue = parsedBody[fieldName]
        if (fieldValue?.isArray == true) {
            return fieldValue.map { it.asText() }
        }
        return emptyList()
    }

    fun getRequestFieldNames(): Set<String> {
        return when (method) {
            HttpMethod.GET.name() -> parameterMap.keys
            else -> parsedBody.keys
        }
    }
}
