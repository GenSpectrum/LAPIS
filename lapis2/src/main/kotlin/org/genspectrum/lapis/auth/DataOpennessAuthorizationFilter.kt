package org.genspectrum.lapis.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.LapisHttpErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

abstract class DataOpennessAuthorizationFilter(val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        when (val result = isAuthorizedForEndpoint(request)) {
            AuthorizationResult.Success -> filterChain.doFilter(request, response)
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

    abstract fun isAuthorizedForEndpoint(request: HttpServletRequest): AuthorizationResult

    companion object {
        fun createFromConfig(databaseConfig: DatabaseConfig, objectMapper: ObjectMapper) =
            when (databaseConfig.schema.opennessLevel) {
                OpennessLevel.OPEN -> NoOpAuthorizationFilter(objectMapper)
                OpennessLevel.GISAID -> ProtectedGisaidDataAuthorizationFilter(objectMapper)
            }
    }
}

sealed interface AuthorizationResult {
    companion object {
        fun success(): AuthorizationResult = Success

        fun failure(message: String): AuthorizationResult = Failure(message)
    }

    fun isSuccessful(): Boolean

    object Success : AuthorizationResult {
        override fun isSuccessful() = true
    }

    class Failure(val message: String) : AuthorizationResult {
        override fun isSuccessful() = false
    }
}

private class NoOpAuthorizationFilter(objectMapper: ObjectMapper) : DataOpennessAuthorizationFilter(objectMapper) {
    override fun isAuthorizedForEndpoint(request: HttpServletRequest) = AuthorizationResult.success()
}

private class ProtectedGisaidDataAuthorizationFilter(objectMapper: ObjectMapper) :
    DataOpennessAuthorizationFilter(objectMapper) {

    override fun isAuthorizedForEndpoint(request: HttpServletRequest) =
        AuthorizationResult.failure("An access key is required to access this endpoint.")
}
