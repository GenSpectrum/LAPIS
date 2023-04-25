package org.genspectrum.lapis.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.genspectrum.lapis.config.AccessKeys
import org.genspectrum.lapis.config.AccessKeysReader
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.LapisHttpErrorResponse
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val ACCESS_KEY_PROPERTY = "accessKey"

private val log = KotlinLogging.logger {}

@Component
class DataOpennessAuthorizationFilterFactory(
    private val databaseConfig: DatabaseConfig,
    private val objectMapper: ObjectMapper,
    private val accessKeysReader: AccessKeysReader,
) {
    fun create() = when (databaseConfig.schema.opennessLevel) {
        OpennessLevel.OPEN -> AlwaysAuthorizedAuthorizationFilter(objectMapper)
        OpennessLevel.GISAID -> ProtectedGisaidDataAuthorizationFilter(
            objectMapper,
            accessKeysReader.read(),
            databaseConfig.schema.metadata.filter { it.unique }.map { it.name },
        )
    }
}

abstract class DataOpennessAuthorizationFilter(protected val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val reReadableRequest = CachedBodyHttpServletRequest(request)

        when (val result = isAuthorizedForEndpoint(reReadableRequest)) {
            AuthorizationResult.Success -> filterChain.doFilter(reReadableRequest, response)
            is AuthorizationResult.Failure -> {
                response.status = HttpStatus.FORBIDDEN.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write(
                    objectMapper.writeValueAsString(
                        LapisHttpErrorResponse(
                            "Forbidden",
                            result.message,
                        ),
                    ),
                )
            }
        }
    }

    abstract fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest): AuthorizationResult
}

sealed interface AuthorizationResult {
    companion object {
        fun success(): AuthorizationResult = Success

        fun failure(message: String): AuthorizationResult = Failure(message)
    }

    object Success : AuthorizationResult

    class Failure(val message: String) : AuthorizationResult
}

private class AlwaysAuthorizedAuthorizationFilter(objectMapper: ObjectMapper) :
    DataOpennessAuthorizationFilter(objectMapper) {

    override fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest) = AuthorizationResult.success()
}

private class ProtectedGisaidDataAuthorizationFilter(
    objectMapper: ObjectMapper,
    private val accessKeys: AccessKeys,
    private val fieldsThatServeNonAggregatedData: List<String>,
) :
    DataOpennessAuthorizationFilter(objectMapper) {

    companion object {
        private val WHITELISTED_PATHS = listOf("/swagger-ui", "/api-docs")
        private val ENDPOINTS_THAT_SERVE_AGGREGATED_DATA = listOf("/aggregated", "/nucleotideMutations")
    }

    override fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest): AuthorizationResult {
        if (WHITELISTED_PATHS.any { request.requestURI.startsWith(it) }) {
            return AuthorizationResult.success()
        }

        val requestFields = getRequestFields(request)

        val accessKey = requestFields[ACCESS_KEY_PROPERTY]
            ?: return AuthorizationResult.failure("An access key is required to access ${request.requestURI}.")

        if (accessKeys.fullAccessKey == accessKey) {
            return AuthorizationResult.success()
        }

        val endpointServesAggregatedData = ENDPOINTS_THAT_SERVE_AGGREGATED_DATA.contains(request.requestURI) &&
            fieldsThatServeNonAggregatedData.intersect(requestFields.keys).isEmpty()

        if (endpointServesAggregatedData && accessKeys.aggregatedDataAccessKey == accessKey) {
            return AuthorizationResult.success()
        }

        return AuthorizationResult.failure("You are not authorized to access ${request.requestURI}.")
    }

    private fun getRequestFields(request: CachedBodyHttpServletRequest): Map<String, String> {
        if (request.parameterNames.hasMoreElements()) {
            return request.parameterMap.mapValues { (_, value) -> value.joinToString() }
        }

        if (request.contentLength == 0) {
            log.warn { "Could not read access key from body, because content length is 0." }
            return emptyMap()
        }

        return try {
            objectMapper.readValue(request.inputStream)
        } catch (exception: Exception) {
            log.error { "Failed to read access key from request body: ${exception.message}" }
            log.debug { exception.stackTraceToString() }
            emptyMap()
        }
    }
}
