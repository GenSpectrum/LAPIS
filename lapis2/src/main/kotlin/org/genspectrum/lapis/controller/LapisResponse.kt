package org.genspectrum.lapis.controller

data class LapisResponse<Data>(val data: Data)

data class LapisErrorResponse(val error: LapisError)
