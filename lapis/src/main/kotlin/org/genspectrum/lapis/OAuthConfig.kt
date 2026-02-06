package org.genspectrum.lapis

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
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
        resourceServerProperties: OAuth2ResourceServerProperties,
    ): SecurityFilterChain {
        val useJwtAuth =
            !resourceServerProperties.jwt.jwkSetUri.isNullOrBlank() ||
                !resourceServerProperties.jwt.issuerUri.isNullOrBlank() ||
                resourceServerProperties.jwt.publicKeyLocation != null

        if (useJwtAuth) {
            log.info { "Configuring JWT authentication for endpoints" }

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
                // we don't need CSRF protection since LAPIS doesn't have state, and we don't send the token in the cookies
                .csrf { it.disable() }
                .oauth2ResourceServer { oauth2 ->
                    oauth2.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(
                            LoggingAuthenticationEntryPoint(BearerTokenAuthenticationEntryPoint()),
                        )
                        .accessDeniedHandler(LoggingAccessDeniedHandler(defaultAccessDeniedHandler))
                }
                .build()
        }

        log.info { "No auth configuration provided, skipping authentication for all endpoints" }
        return httpSecurity
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
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
