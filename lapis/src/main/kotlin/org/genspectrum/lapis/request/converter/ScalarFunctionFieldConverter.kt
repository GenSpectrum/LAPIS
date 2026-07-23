package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.ComputedField
import org.genspectrum.lapis.request.ScalarFunction
import org.springframework.stereotype.Component

@Component
class ScalarFunctionFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
    private val databaseConfig: DatabaseConfig,
) {
    private val fieldTypesByLowercaseName: Map<String, MetadataType> =
        databaseConfig.schema.metadata.associateBy({ it.name.lowercase() }, { it.type })

    fun tryConvert(source: String): ComputedField? {
        if ('.' !in source) return null
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

        return ComputedField(cleanedField, function)
    }
}
