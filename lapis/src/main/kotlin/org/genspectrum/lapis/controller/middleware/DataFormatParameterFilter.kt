package org.genspectrum.lapis.controller.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisMediaType
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_NEWICK
import org.genspectrum.lapis.request.FORMAT_PROPERTY
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.genspectrum.lapis.util.HeaderModifyingRequestWrapper
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val HEADERS_ACCEPT_HEADER_PARAMETER = "headers"
const val ESCAPED_ACCEPT_HEADER_PARAMETER = "escaped"

object DataFormat {
    const val JSON = "JSON"
    const val CSV = "CSV"
    const val CSV_WITHOUT_HEADERS = "CSV-WITHOUT-HEADERS"
    const val TSV = "TSV"
    const val NEWICK = "NEWICK"
}

enum class TreeDataFormat(
    val value: String,
) {
    NEWICK(DataFormat.NEWICK),
    ;

    companion object {
        fun fromAcceptHeaders(acceptHeaders: List<MediaType>): TreeDataFormat {
            for (acceptHeader in acceptHeaders) {
                if (TEXT_NEWICK.includes(acceptHeader)) {
                    return NEWICK
                }
            }
            return NEWICK
        }
    }
    const val TSV_ESCAPED = "TSV-ESCAPED"
}

enum class SequencesDataFormat(
    val value: String,
) {
    FASTA("FASTA"),
    JSON(DataFormat.JSON),
    NDJSON("NDJSON"),
    ;

    companion object {
        fun fromAcceptHeaders(acceptHeaders: List<MediaType>): SequencesDataFormat {
            for (acceptHeader in acceptHeaders) {
                if (TEXT_X_FASTA.includes(acceptHeader)) {
                    return FASTA
                }

                if (MediaType.APPLICATION_JSON.includes(acceptHeader)) {
                    return JSON
                }

                if (MediaType.APPLICATION_NDJSON.includes(acceptHeader)) {
                    return NDJSON
                }
            }
            return FASTA
        }
    }
}

@Component
@Order(DATA_FORMAT_FILTER_ORDER)
class DataFormatParameterFilter(
    val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest.from(request, objectMapper)

        val requestWithModifiedAcceptHeader = HeaderModifyingRequestWrapper(
            reReadableRequest,
            ACCEPT,
            ::findAcceptHeaderOverwriteValue,
        )

        filterChain.doFilter(
            requestWithModifiedAcceptHeader,
            response,
        )
    }

    private fun findAcceptHeaderOverwriteValue(reReadableRequest: CachedBodyHttpServletRequest) =
        when (reReadableRequest.getStringField(FORMAT_PROPERTY)?.uppercase()) {
            null -> null
            DataFormat.CSV -> LapisMediaType.TEXT_CSV_VALUE
            DataFormat.CSV_WITHOUT_HEADERS -> LapisMediaType.TEXT_CSV_WITHOUT_HEADERS_VALUE
            DataFormat.TSV -> LapisMediaType.TEXT_TSV_VALUE
            DataFormat.TSV_ESCAPED -> LapisMediaType.TEXT_TSV_ESCAPED_VALUE
            DataFormat.JSON -> MediaType.APPLICATION_JSON_VALUE
            SequencesDataFormat.FASTA.value -> LapisMediaType.TEXT_X_FASTA_VALUE
            SequencesDataFormat.NDJSON.value -> MediaType.APPLICATION_NDJSON_VALUE

            else -> "unknown/unknown"
        }
}
