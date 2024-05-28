package org.genspectrum.lapis.controller

import org.genspectrum.lapis.request.LapisInfo
import org.springframework.http.ProblemDetail

data class LapisResponse<Data>(val data: Data, val info: LapisInfo = LapisInfo())

data class LapisErrorResponse(val error: ProblemDetail, val info: LapisInfo = LapisInfo())
