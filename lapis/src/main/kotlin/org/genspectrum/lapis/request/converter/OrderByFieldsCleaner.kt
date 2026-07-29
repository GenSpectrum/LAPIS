package org.genspectrum.lapis.request.converter

import org.springframework.stereotype.Component

@Component
class OrderByFieldsCleaner(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
) {
    fun clean(fieldName: String): String = caseInsensitiveFieldsCleaner.clean(fieldName) ?: fieldName
}
