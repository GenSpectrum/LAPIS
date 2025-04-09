package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

data class Field(
    val fieldName: String,
)

fun interface FieldConverter<T> {
    fun convert(source: String): T
}

@Component
class CaseInsensitiveFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
) : FieldConverter<Field> {
    override fun convert(source: String): Field {
        val cleaned = caseInsensitiveFieldsCleaner.clean(source)
            ?: throw BadRequestException(
                "Unknown field: '$source', known values are ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )

        return Field(cleaned)
    }
}
