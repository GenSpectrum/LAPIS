package org.genspectrum.lapis.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
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

    fun getRequestFields(): Map<String, JsonNode> {
        if (parameterNames.hasMoreElements()) {
            return parameterMap.mapValues { (_, value) -> TextNode(value.joinToString()) }
        }

        if (contentLength == 0) {
            log.debug { "Could not read from request body, because content length is 0." }
            return emptyMap()
        }

        return try {
            objectMapper.readValue(inputStream)
        } catch (exception: Exception) {
            if (method != HttpMethod.GET.name()) {
                log.warn { "Failed to read from request body: ${exception.message}, contentLength $contentLength" }
                log.debug { exception.stackTraceToString() }
            }
            emptyMap()
        }
    }
}
