package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.LineageWithSublineagesField
import org.springframework.stereotype.Component

/**
 * Recognizes the sublineage-inclusive aggregation syntax `<lineageField>*` (e.g. `pangoLineage*`) used by
 * `/aggregated`. [tryConvert] returns `null` for strings without a trailing `*`, so callers can fall back to
 * plain field handling; it throws when the field before the `*` is unknown or has no lineage index.
 */
@Component
class LineageFieldConverter(
    databaseConfig: DatabaseConfig,
) {
    private val lineageFieldsByLowercaseName: Map<String, String> =
        databaseConfig.schema.metadata
            .filter { it.generateLineageIndex != null }
            .associateBy({ it.name.lowercase() }, { it.name })

    fun tryConvert(source: String): LineageWithSublineagesField? {
        if (!source.endsWith('*')) {
            return null
        }

        val rawField = source.substring(0, source.length - 1)
        val cleaned = lineageFieldsByLowercaseName[rawField.lowercase()]
            ?: throw BadRequestException(
                "Invalid field '$source': the '*' suffix for sublineage-inclusive aggregation is only allowed " +
                    "for fields with a lineage index, which are: ${lineageFieldsByLowercaseName.values}",
            )
        return LineageWithSublineagesField(cleaned)
    }
}
