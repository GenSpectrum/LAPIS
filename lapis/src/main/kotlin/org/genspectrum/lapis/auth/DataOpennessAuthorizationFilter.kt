package org.genspectrum.lapis.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.middleware.DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER
import org.genspectrum.lapis.response.LapisErrorResponse
import org.genspectrum.lapis.response.LapisInfoFactory
import org.genspectrum.lapis.util.CachedBodyHttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class DataOpennessAuthorizationFilterFactory(
    private val databaseConfig: DatabaseConfig,
    private val objectMapper: ObjectMapper,
    private val lapisInfoFactory: LapisInfoFactory,
) {
    fun create(): DataOpennessAuthorizationFilter =
        when (databaseConfig.schema.opennessLevel) {
            OpennessLevel.OPEN -> AlwaysAuthorizedAuthorizationFilter(
                objectMapper = objectMapper,
                lapisInfoFactory = lapisInfoFactory,
            )
        }
}

@Order(DATA_OPENNESS_AUTHORIZATION_FILTER_ORDER)
abstract class DataOpennessAuthorizationFilter(
    protected val objectMapper: ObjectMapper,
    private val lapisInfoFactory: LapisInfoFactory,
) : OncePerRequestFilter() {
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
                            error = ProblemDetail.forStatus(HttpStatus.FORBIDDEN).also {
                                it.title = HttpStatus.FORBIDDEN.reasonPhrase
                                it.detail = result.message
                            },
                            info = lapisInfoFactory.create(),
                        ),
                    ),
                )
            }
        }
    }

    private fun makeRequestBodyReadableMoreThanOnce(request: HttpServletRequest) =
        CachedBodyHttpServletRequest.from(request, objectMapper)

    abstract fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest): AuthorizationResult
}

sealed interface AuthorizationResult {
    companion object {
        fun success(): AuthorizationResult = Success

        fun failure(message: String): AuthorizationResult = Failure(message)
    }

    data object Success : AuthorizationResult

    class Failure(
        val message: String,
    ) : AuthorizationResult
}

private class AlwaysAuthorizedAuthorizationFilter(
    objectMapper: ObjectMapper,
    lapisInfoFactory: LapisInfoFactory,
) : DataOpennessAuthorizationFilter(objectMapper, lapisInfoFactory) {
    override fun isAuthorizedForEndpoint(request: CachedBodyHttpServletRequest) = AuthorizationResult.success()
}
