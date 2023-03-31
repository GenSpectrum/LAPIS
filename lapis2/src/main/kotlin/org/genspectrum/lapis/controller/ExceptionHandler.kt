package org.genspectrum.lapis.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.genspectrum.lapis.silo.SiloException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Throwable::class)
    fun handleUnexpectedException(e: Throwable): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Unexpected error",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Bad request",
                        "${e.message}",
                    ),
                ),
            )
    }

    @ExceptionHandler(SiloException::class)
    fun handleSiloException(e: SiloException): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                jacksonObjectMapper().writeValueAsString(
                    LapisHttpErrorResponse(
                        "Silo error",
                        "${e.message}",
                    ),
                ),
            )
    }
}

data class LapisHttpErrorResponse(val title: String, val message: String)
