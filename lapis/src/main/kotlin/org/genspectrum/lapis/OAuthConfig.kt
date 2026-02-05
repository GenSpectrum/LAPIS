package org.genspectrum.lapis

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.access.DelegatingAccessDeniedHandler
import org.springframework.security.web.csrf.CsrfException

private const val AUTH_URL_PROPERTY = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"

@Configuration
@EnableWebSecurity
class OAuthConfig {
    // This is the preconfigured default that we want to wrap in a logger
    private val defaultAccessDeniedHandler = DelegatingAccessDeniedHandler(
        linkedMapOf(CsrfException::class.java to AccessDeniedHandlerImpl()),
        BearerTokenAccessDeniedHandler(),
    )

    @Bean
    fun securityFilterChain(
        httpSecurity: HttpSecurity,
        @Value("\${$AUTH_URL_PROPERTY:#{null}}") authUrlProperty: String?,
    ): SecurityFilterChain {
        if (authUrlProperty == null) {
            log.info { "No $AUTH_URL_PROPERTY provided, skipping authentication for all endpoints" }
            return httpSecurity
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .csrf { it.disable() }
                .build()
        }

        log.info { "Configuring authentication for endpoints, using $AUTH_URL_PROPERTY=$authUrlProperty" }

        return httpSecurity
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/",
                    "/favicon.ico",
                    "/error/**",
                    "/actuator/**",
                    "/api-docs**",
                    "/api-docs/**",
                    "/swagger-ui/**",
                ).permitAll()
                auth.requestMatchers(HttpMethod.OPTIONS).permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(
                        LoggingAuthenticationEntryPoint(BearerTokenAuthenticationEntryPoint()),
                    )
                    .accessDeniedHandler(LoggingAccessDeniedHandler(defaultAccessDeniedHandler))
            }
            .build()
    }
}

class LoggingAuthenticationEntryPoint(
    private val entryPoint: AuthenticationEntryPoint,
) : AuthenticationEntryPoint by entryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        log.warn { "${request.method} ${request.requestURI}: $authException" }
        entryPoint.commence(request, response, authException)
    }
}

class LoggingAccessDeniedHandler(
    private val accessDeniedHandler: AccessDeniedHandler,
) : AccessDeniedHandler by accessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        log.warn { "${request.method} ${request.requestURI}: $accessDeniedException" }
        accessDeniedHandler.handle(request, response, accessDeniedException)
    }
}
