package org.genspectrum.lapis.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.AccessKeys
import org.genspectrum.lapis.config.AccessKeysReader
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.ACCESS_KEY_PROPERTY
import org.genspectrum.lapis.controller.AGGREGATED_ROUTE
import org.genspectrum.lapis.controller.AMINO_ACID_INSERTIONS_ROUTE
import org.genspectrum.lapis.controller.AMINO_ACID_MUTATIONS_ROUTE
import org.genspectrum.lapis.controller.DATABASE_CONFIG_ROUTE
import org.genspectrum.lapis.controller.FIELDS_PROPERTY
import org.genspectrum.lapis.controller.INFO_ROUTE
import org.genspectrum.lapis.controller.LapisErrorResponse
import org.genspectrum.lapis.controller.NUCLEOTIDE_INSERTIONS_ROUTE
import org.genspectrum.lapis.controller.NUCLEOTIDE_MUTATIONS_ROUTE
import org.genspectrum.lapis.controller.REFERENCE_GENOME_ROUTE
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class DataOpennessAuthorizationFilterFactory(
    private val databaseConfig: DatabaseConfig,
    private val objectMapper: ObjectMapper,
    private val accessKeysReader: AccessKeysReader,
) {
    fun create() =
        when (databaseConfig.schema.opennessLevel) {
            OpennessLevel.OPEN -> AlwaysAuthorizedAuthorizationFilter(objectMapper)
            OpennessLevel.PROTECTED -> ProtectedDataAuthorizationFilter(
                objectMapper,
                accessKeysReader.read(),
                databaseConfig,
            )
        }
}

abstract class DataOpennessAuthorizationFilter(protected val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = makeRequestBodyReadableMoreThanOnce(request)

        when (val result = isAuthorizedForEndpoint(reReadableRequest)) {
            AuthorizationResult.Success -> filterChain.doFilter(reReadableRequest, response)
            is AuthorizationResult.Failure -> {
                response.status = HttpStatus.FORBIDDEN.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write(
                    objectMapper.writeValueAsString(
                        LapisErrorResponse(
                            ProblemDetail.forStatus(HttpStatus.FORBIDDEN).also {
                                it.title = HttpStatus.FORBIDDEN.reasonPhrase
                                it.detail = result.message
                            },
                        ),
                    ),
                )
            }
        }
    }

    private fun makeRequestBodyReadableMoreThanOnce(request: HttpServletRequest) =
        CachedBodyHttpServletRequest(request, objectMapper)

    abstract fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest): AuthorizationResult
}

sealed interface AuthorizationResult {
    companion object {
        fun success(): AuthorizationResult = Success

        fun failure(message: String): AuthorizationResult = Failure(message)
    }

    data object Success : AuthorizationResult

    class Failure(val message: String) : AuthorizationResult
}

private class AlwaysAuthorizedAuthorizationFilter(objectMapper: ObjectMapper) :
    DataOpennessAuthorizationFilter(objectMapper) {
    override fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest) = AuthorizationResult.success()
}

private class ProtectedDataAuthorizationFilter(
    objectMapper: ObjectMapper,
    private val accessKeys: AccessKeys,
    private val databaseConfig: DatabaseConfig,
) :
    DataOpennessAuthorizationFilter(objectMapper) {
    private val fieldsThatServeNonAggregatedData = databaseConfig.schema
        .metadata
        .filter { it.valuesAreUnique }
        .map { it.name }

    companion object {
        private val WHITELISTED_PATH_PREFIXES = listOf(
            "/swagger-ui",
            "/api-docs",
            "/sample$DATABASE_CONFIG_ROUTE",
            "/sample$REFERENCE_GENOME_ROUTE",
        )
        private val ENDPOINTS_THAT_SERVE_AGGREGATED_DATA = listOf(
            AGGREGATED_ROUTE,
            NUCLEOTIDE_MUTATIONS_ROUTE,
            AMINO_ACID_MUTATIONS_ROUTE,
            NUCLEOTIDE_INSERTIONS_ROUTE,
            AMINO_ACID_INSERTIONS_ROUTE,
            INFO_ROUTE,
        ).map { "/sample$it" }
    }

    override fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest): AuthorizationResult {
        val path = request.getProxyAwarePath()

        if (path == "/" || WHITELISTED_PATH_PREFIXES.any { path.startsWith(it) }) {
            return AuthorizationResult.success()
        }

        val accessKey = request.getStringField(ACCESS_KEY_PROPERTY)
            ?: return AuthorizationResult.failure("An access key is required to access $path.")

        if (accessKeys.fullAccessKeys.contains(accessKey)) {
            return AuthorizationResult.success()
        }

        if (accessKeys.aggregatedDataAccessKeys.contains(accessKey) && endpointServesAggregatedData(request)) {
            return AuthorizationResult.success()
        }

        return AuthorizationResult.failure("You are not authorized to access $path.")
    }

    private fun endpointServesAggregatedData(request: CachedBodyHttpServletRequest): Boolean {
        val fields = request.getStringArrayField(FIELDS_PROPERTY)
        if (containsOnlyPrimaryKey(fields)) {
            return true
        }

        if (!ENDPOINTS_THAT_SERVE_AGGREGATED_DATA.contains(request.getProxyAwarePath())) {
            return false
        }

        if (fieldsThatServeNonAggregatedData.intersect(request.getRequestFieldNames()).isNotEmpty()) {
            return false
        }

        return fields.intersect(fieldsThatServeNonAggregatedData.toSet()).isEmpty()
    }

    private fun containsOnlyPrimaryKey(fields: List<String>) =
        fields.size == 1 && fields.first() == databaseConfig.schema.primaryKey
}
