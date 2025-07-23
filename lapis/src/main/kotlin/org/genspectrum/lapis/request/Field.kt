package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

data class Field(
    val fieldName: String,
)

fun interface FieldConverter<T> {
    fun convert(source: String): T

    fun validatePhyloTreeFields(source: String) {
        // Default implementation does nothing, can be overridden if needed
    }
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

    override fun validatePhyloTreeFields(source: String) {
        val converted = convert(source)
        val validFields = caseInsensitiveFieldsCleaner.getPhyloTreeFields()
        if (converted.fieldName !in validFields) {
            throw BadRequestException(
                "Field '${converted.fieldName}' is not a phylo tree field, " +
                    "known phylo tree fields are ${validFields.joinToString(
                        ", ",
                    )}",
            )
        }
    }
}
