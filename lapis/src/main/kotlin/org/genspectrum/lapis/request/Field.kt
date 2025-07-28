package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

data class Field(
    val fieldName: String,
)

fun interface FieldConverter<T> {
    fun convert(source: String): T

    fun validatePhyloTreeField(source: String): T = convert(source)
}

@Component
class CaseInsensitiveFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
    private val databaseConfig: DatabaseConfig,
) : FieldConverter<Field> {
    override fun convert(source: String): Field {
        val cleaned = caseInsensitiveFieldsCleaner.clean(source)
            ?: throw BadRequestException(
                "Unknown field: '$source', known values are ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )

        return Field(cleaned)
    }

    override fun validatePhyloTreeField(source: String): Field {
        val converted = convert(source)
        val validFields = databaseConfig.schema.metadata.filter { it.phyloTreeNodeIdentifier }.map { it.name }
        if (converted.fieldName !in validFields) {
            throw BadRequestException(
                "Field '${converted.fieldName}' is not a phylo tree field, " +
                    "known phylo tree fields are [${validFields.joinToString(
                        ", ",
                    )}]",
            )
        }
        return converted
    }
}
