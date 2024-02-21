package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.luben.zstd.ZstdOutputStream
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.HeaderModifyingRequestWrapper
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.web.servlet.server.Encoding
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.filter.OncePerRequestFilter
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Enumeration
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

enum class Compression(val value: String, val compressionOutputStreamFactory: (OutputStream) -> OutputStream) {
    GZIP("gzip", ::LazyGzipOutputStream),
    ZSTD("zstd", { ZstdOutputStream(it).apply { commitUnderlyingResponseToPreventContentLengthFromBeingSet() } }),
    ;

    companion object {
        fun fromHeaders(acceptEncodingHeaders: Enumeration<String>?): Compression? {
            if (acceptEncodingHeaders == null) {
                return null
            }

            val headersList = acceptEncodingHeaders.toList()

            return when {
                headersList.contains(GZIP.value) -> GZIP
                headersList.contains(ZSTD.value) -> ZSTD
                else -> null
            }
        }
    }
}

class LazyGzipOutputStream(outputStream: OutputStream) : OutputStream() {
    private val gzipOutputStream by lazy { GZIPOutputStream(outputStream) }

    override fun write(byte: Int) = gzipOutputStream.write(byte)

    override fun write(bytes: ByteArray) = gzipOutputStream.write(bytes)

    override fun write(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) = gzipOutputStream.write(bytes, offset, length)

    override fun flush() = gzipOutputStream.flush()

    override fun close() = gzipOutputStream.close()
}

// https://github.com/apache/tomcat/blob/10e3731f344cd0d018d4be2ee767c105d2832283/java/org/apache/catalina/connector/OutputBuffer.java#L223-L229
fun ZstdOutputStream.commitUnderlyingResponseToPreventContentLengthFromBeingSet() {
    val nothing = ByteArray(0)
    write(nothing)
}

@Component
@RequestScope
class RequestCompression(var compression: Compression? = null)

@Component
@Order(COMPRESSION_FILTER_ORDER)
class CompressionFilter(val objectMapper: ObjectMapper, val requestCompression: RequestCompression) :
    OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest(request, objectMapper)

        try {
            validateCompressionProperty(reReadableRequest)
        } catch (e: UnknownCompressionFormatException) {
            response.status = HttpStatus.BAD_REQUEST.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                objectMapper.writeValueAsString(
                    LapisErrorResponse(
                        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
                            title = HttpStatus.BAD_REQUEST.reasonPhrase
                            detail = "Unknown compression format: ${e.unknownFormatValue}. " +
                                "Supported formats are: ${Compression.entries.joinToString { it.value }}"
                        },
                    ),
                ),
            )
            return
        }

        val requestWithContentEncoding = HeaderModifyingRequestWrapper(
            reReadableRequest = reReadableRequest,
            headerName = ACCEPT_ENCODING,
            computeHeaderValueFromRequest = ::computeAcceptEncodingValueFromRequest,
        )

        val maybeCompressingResponse = createMaybeCompressingResponse(
            response,
            requestWithContentEncoding.getHeaders(ACCEPT_ENCODING),
        )

        filterChain.doFilter(
            requestWithContentEncoding,
            maybeCompressingResponse,
        )

        maybeCompressingResponse.outputStream.flush()
        maybeCompressingResponse.outputStream.close()
    }

    private fun validateCompressionProperty(reReadableRequest: CachedBodyHttpServletRequest) {
        val compressionFormat = reReadableRequest.getStringField(COMPRESSION_PROPERTY) ?: return

        if (Compression.entries.toSet().none { it.value == compressionFormat }) {
            throw UnknownCompressionFormatException(unknownFormatValue = compressionFormat)
        }
    }

    private fun createMaybeCompressingResponse(
        response: HttpServletResponse,
        acceptEncodingHeaders: Enumeration<String>?,
    ) = when (val compression = Compression.fromHeaders(acceptEncodingHeaders)) {
        null -> response
        else -> {
            requestCompression.compression = compression
            CompressingResponse(response, compression)
        }
    }
}

private fun computeAcceptEncodingValueFromRequest(reReadableRequest: CachedBodyHttpServletRequest) =
    when (reReadableRequest.getStringField(COMPRESSION_PROPERTY)) {
        Compression.GZIP.value -> Compression.GZIP.value
        Compression.ZSTD.value -> Compression.ZSTD.value
        else -> null
    }

private class UnknownCompressionFormatException(val unknownFormatValue: String) : Exception()

class CompressingResponse(
    response: HttpServletResponse,
    compression: Compression,
) : HttpServletResponse by response {
    init {
        log.info { "Compressing using $compression" }
        response.setHeader(CONTENT_ENCODING, compression.value)
    }

    private val servletOutputStream = CompressingServletOutputStream(response.outputStream, compression)

    override fun getOutputStream() = servletOutputStream
}

class CompressingServletOutputStream(
    private val outputStream: ServletOutputStream,
    compression: Compression,
) : ServletOutputStream() {
    private val compressingStream = compression.compressionOutputStreamFactory(outputStream)

    override fun write(byte: Int) {
        compressingStream.write(byte)
    }

    override fun isReady() = outputStream.isReady

    override fun setWriteListener(listener: WriteListener?) = outputStream.setWriteListener(listener)

    override fun close() {
        super.close()
        compressingStream.close()
    }

    override fun flush() {
        super.flush()
        compressingStream.flush()
    }
}

@Component
class StringHttpMessageConverterWithUnknownContentLengthInCaseOfCompression(
    environment: Environment,
    val requestCompression: RequestCompression,
) : StringHttpMessageConverter(getCharsetFromEnvironment(environment)) {
    override fun getContentLength(
        str: String,
        contentType: MediaType?,
    ): Long? {
        return when (requestCompression.compression) {
            null -> super.getContentLength(str, contentType)
            else -> null
        }
    }

    companion object {
        // taken from the initialization in
        // org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration
        // since this class replaces that one
        private fun getCharsetFromEnvironment(environment: Environment): Charset =
            Binder.get(environment)
                .bindOrCreate("server.servlet.encoding", Encoding::class.java)
                .charset
    }
}
