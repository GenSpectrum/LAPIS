package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

sealed class Field {
    /** The name as it appears in API requests and responses. */
    abstract val fieldName: String

    data class Plain(
        override val fieldName: String,
    ) : Field()

    data class Computed(
        val sourceField: String,
        val function: ScalarFunction,
    ) : Field() {
        override val fieldName = "$sourceField.${function.saneQlMethodName}"

        /** Column name used internally in the SaneQL map step; never exposed to API callers. */
        val alias = "__scalar_${function.saneQlMethodName}_$sourceField"
    }
}

fun interface FieldConverter<T> {
    fun convert(source: String): T

    fun validatePhyloTreeFields(source: String): T = convert(source)
}

@Component
class CaseInsensitiveFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
    private val databaseConfig: DatabaseConfig,
) : FieldConverter<Field> {
    private val fieldTypesByLowercaseName: Map<String, MetadataType> =
        databaseConfig.schema.metadata.associateBy({ it.name.lowercase() }, { it.type })

    override fun convert(source: String): Field {
        if ('.' in source) {
            return convertComputedField(source)
        }
        val cleaned = caseInsensitiveFieldsCleaner.clean(source)
            ?: throw BadRequestException(
                "Unknown field: '$source', known values are ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )
        return Field.Plain(cleaned)
    }

    private fun convertComputedField(source: String): Field.Computed {
        val dotIndex = source.lastIndexOf('.')
        val rawField = source.substring(0, dotIndex)
        val rawFunction = source.substring(dotIndex + 1)

        val cleanedField = caseInsensitiveFieldsCleaner.clean(rawField)
            ?: throw BadRequestException(
                "Unknown field '$rawField' in '$source'. " +
                    "Known fields: ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )

        val function = ScalarFunction.entries.find { it.saneQlMethodName.equals(rawFunction, ignoreCase = true) }
            ?: throw BadRequestException(
                "Unknown scalar function '$rawFunction' in '$source'. " +
                    "Available functions: ${ScalarFunction.entries.joinToString { it.saneQlMethodName }}",
            )

        val fieldType = fieldTypesByLowercaseName[cleanedField.lowercase()]!!
        if (fieldType !in function.validForTypes) {
            throw BadRequestException(
                "Scalar function '${function.saneQlMethodName}' is not valid for field '$cleanedField' of type " +
                    "$fieldType. Valid types: ${function.validForTypes.joinToString()}",
            )
        }

        return Field.Computed(cleanedField, function)
    }

    override fun validatePhyloTreeFields(source: String): Field {
        val converted = convert(source)
        val validFields = caseInsensitiveFieldsCleaner.getPhyloTreeFields()
        if (converted.fieldName !in validFields) {
            throw BadRequestException(
                "Field '${converted.fieldName}' is not a phylo tree field, " +
                    "known phylo tree fields are [${validFields.joinToString(", ")}]",
            )
        }
        return converted
    }
}

fun validatePhyloTreeField(
    source: String,
    fieldConverter: FieldConverter<Field>,
    databaseConfig: DatabaseConfig,
): Field {
    val converted = fieldConverter.convert(source)
    val validFields = databaseConfig.schema.metadata.filter { it.isPhyloTreeField }.map { it.name }
    if (converted.fieldName !in validFields) {
        throw BadRequestException(
            "Field '${converted.fieldName}' is not a phylo tree field, " +
                "known phylo tree fields are [${validFields.joinToString(", ")}]",
        )
    }
    return converted
}
