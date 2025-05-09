package org.genspectrum.lapis.controller.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.luben.zstd.ZstdOutputStream
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.request.COMPRESSION_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.response.LapisErrorResponse
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.ResponseWithContentType
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.web.servlet.server.Encoding
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Enumeration
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

enum class Compression(
    val value: String,
    val contentType: MediaType,
    val compressionOutputStreamFactory: (OutputStream) -> OutputStream,
    val fileEnding: String,
) {
    GZIP(
        value = "gzip",
        contentType = MediaType.parseMediaType("application/gzip"),
        compressionOutputStreamFactory = ::LazyGzipOutputStream,
        fileEnding = ".gz",
    ),
    ZSTD(
        value = "zstd",
        contentType = MediaType.parseMediaType("application/zstd"),
        compressionOutputStreamFactory = {
            ZstdOutputStream(it).apply { commitUnderlyingResponseToPreventContentLengthFromBeingSet() }
        },
        fileEnding = ".zst",
    ),
    ;

    companion object {
        fun fromHeaders(acceptEncodingHeaders: Enumeration<String>?): Compression? {
            if (acceptEncodingHeaders == null) {
                return null
            }

            val headersList = acceptEncodingHeaders.toList()
                .flatMap { it.split(',') }
                .map { it.trim() }

            return when {
                headersList.contains(ZSTD.value) -> ZSTD
                headersList.contains(GZIP.value) -> GZIP
                else -> null
            }
        }
    }
}

class LazyGzipOutputStream(
    outputStream: OutputStream,
) : OutputStream() {
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
class RequestCompression(
    var compressionSource: CompressionSource = CompressionSource.None,
)

sealed interface CompressionSource {
    data class RequestProperty(
        override var compression: Compression,
    ) : CompressionSource

    data class AcceptEncodingHeader(
        override var compression: Compression,
    ) : CompressionSource

    data object None : CompressionSource {
        override val compression = null
    }

    val compression: Compression?
}

@Component
@Order(COMPRESSION_FILTER_ORDER)
class CompressionFilter(
    private val objectMapper: ObjectMapper,
    private val requestCompression: RequestCompression,
    private val lapisInfoFactory: LapisInfoFactory,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest.from(request, objectMapper)

        val compressionPropertyInRequest = try {
            getValidatedCompressionProperty(reReadableRequest)
        } catch (e: UnknownCompressionFormatException) {
            response.status = HttpStatus.BAD_REQUEST.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                objectMapper.writeValueAsString(
                    LapisErrorResponse(
                        error = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
                            title = HttpStatus.BAD_REQUEST.reasonPhrase
                            detail = "Unknown compression format: ${e.unknownFormatValue}. " +
                                "Supported formats are: ${Compression.entries.joinToString { it.value }}"
                        },
                        info = lapisInfoFactory.create(),
                    ),
                ),
            )
            return
        }

        val maybeCompressingResponse = createMaybeCompressingResponse(
            response,
            reReadableRequest,
            compressionPropertyInRequest,
        )

        filterChain.doFilter(
            reReadableRequest,
            maybeCompressingResponse,
        )

        try {
            maybeCompressingResponse.outputStream.flush()
            maybeCompressingResponse.outputStream.close()
        } catch (e: IOException) {
            log.debug { "Failed to flush and close the compressing output stream: ${e.message}" }
        }
    }

    private fun getValidatedCompressionProperty(reReadableRequest: CachedBodyHttpServletRequest): Compression? {
        val compressionFormat = reReadableRequest.getStringField(COMPRESSION_PROPERTY) ?: return null

        return Compression.entries.toSet().find { it.value == compressionFormat }
            ?: throw UnknownCompressionFormatException(unknownFormatValue = compressionFormat)
    }

    private fun createMaybeCompressingResponse(
        response: HttpServletResponse,
        reReadableRequest: CachedBodyHttpServletRequest,
        compressionPropertyInRequest: Compression?,
    ): HttpServletResponse {
        if (compressionPropertyInRequest != null) {
            log.info { "Compressing using $compressionPropertyInRequest from request property" }

            requestCompression.compressionSource = CompressionSource.RequestProperty(compressionPropertyInRequest)
            return CompressingResponse(
                ResponseWithContentType(response, compressionPropertyInRequest.contentType.toString()),
                compressionPropertyInRequest,
            )
        }

        val downloadAsFile = reReadableRequest.getBooleanField(DOWNLOAD_AS_FILE_PROPERTY) ?: false
        if (downloadAsFile) {
            return response
        }
        if (!reReadableRequest.getProxyAwarePath().startsWith("/sample")) {
            return response
        }

        val acceptEncodingHeaders = reReadableRequest.getHeaders(ACCEPT_ENCODING)
        val compression = Compression.fromHeaders(acceptEncodingHeaders) ?: return response

        log.info { "Compressing using $compression from $ACCEPT_ENCODING header" }

        requestCompression.compressionSource = CompressionSource.AcceptEncodingHeader(compression)
        return CompressingResponse(response, compression)
            .apply {
                setHeader(CONTENT_ENCODING, compression.value)
            }
    }
}

private class UnknownCompressionFormatException(
    val unknownFormatValue: String,
) : Exception()

class CompressingResponse(
    private val response: HttpServletResponse,
    compression: Compression,
) : HttpServletResponse by response {
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

    override fun write(bytes: ByteArray) {
        compressingStream.write(bytes)
    }

    override fun write(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) {
        compressingStream.write(bytes, offset, length)
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
class CompressionAwareMappingJackson2HttpMessageConverter(
    objectMapper: ObjectMapper,
    private val requestCompression: RequestCompression,
) : MappingJackson2HttpMessageConverter(objectMapper) {
    override fun canWrite(mediaType: MediaType?): Boolean {
        if (requestCompression.compressionSource.compression?.contentType?.isCompatibleWith(mediaType) == true) {
            return true
        }

        return super.canWrite(mediaType)
    }

    override fun addDefaultHeaders(
        headers: HttpHeaders,
        value: Any,
        contentType: MediaType?,
    ) {
        val compressionSource = requestCompression.compressionSource
        if (
            compressionSource is CompressionSource.RequestProperty &&
            compressionSource.compression.contentType != contentType
        ) {
            headers.set(CONTENT_ENCODING, compressionSource.compression.value)
        }

        super.addDefaultHeaders(headers, value, contentType)
    }
}

@Component
class StringHttpMessageConverterWithUnknownContentLengthInCaseOfCompression(
    environment: Environment,
    private val requestCompression: RequestCompression,
) : StringHttpMessageConverter(getCharsetFromEnvironment(environment)) {
    // The original method is declared in Java as returning Long (can be null)
    // but in Kotlin it is Long! (platform type) which is not nullable.
    @Suppress("INCOMPATIBLE_OVERRIDE")
    override fun getContentLength(
        str: String,
        contentType: MediaType?,
    ): Long? =
        when (requestCompression.compressionSource.compression) {
            null -> super.getContentLength(str, contentType)
            else -> null
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
