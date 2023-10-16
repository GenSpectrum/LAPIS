package org.genspectrum.lapis.controller

import org.springframework.http.ProblemDetail

data class LapisResponse<Data>(val data: Data)

data class LapisErrorResponse(val error: ProblemDetail)
