package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

@Component
class OrderByFieldsCleaner(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
    private val scalarFunctionFieldConverter: ScalarFunctionFieldConverter,
    private val lineageFieldConverter: LineageFieldConverter,
) {
    fun clean(fieldName: String): String =
        try {
            lineageFieldConverter.tryConvert(fieldName)?.outputColumnName
                ?: scalarFunctionFieldConverter.tryConvert(fieldName)?.outputColumnName
                ?: caseInsensitiveFieldsCleaner.clean(fieldName)
                ?: fieldName
        } catch (e: BadRequestException) {
            fieldName
        }
}
