package org.genspectrum.lapis.controller

import io.jsonwebtoken.Jwts
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import java.security.KeyPair
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

val keyPair: KeyPair = Jwts.SIG.RS256.keyPair().build()

val validJwt = generateJwtFor("dummy user")

fun generateJwtFor(username: String): String =
    Jwts.builder()
        .expiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
        .issuedAt(Date.from(Instant.now()))
        .signWith(keyPair.private, Jwts.SIG.RS256)
        .claim("preferred_username", username)
        .compact()

fun MockHttpServletRequestBuilder.withAuth(bearerToken: String): MockHttpServletRequestBuilder =
    this.header("Authorization", "Bearer $bearerToken")
