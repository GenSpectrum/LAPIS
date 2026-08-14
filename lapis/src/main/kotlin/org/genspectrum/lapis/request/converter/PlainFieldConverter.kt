package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.PlainField
import org.springframework.stereotype.Component

/** Resolves a plain metadata field, rejecting sequence position fields with a clear error. */
@Component
class PlainFieldConverter(
    private val sequencePositionFieldConverter: SequencePositionFieldConverter,
    private val metadataFieldConverter: MetadataFieldConverter,
) : FieldConverter<PlainField> {
    override fun convert(source: String): PlainField {
        sequencePositionFieldConverter.tryConvert(source)?.let {
            throw BadRequestException(
                "Sequence position fields are not supported here: ${it.outputColumnName}",
            )
        }
        if ('.' in source) {
            throw BadRequestException(
                "Scalar functions are not supported in fields for this endpoint: $source",
            )
        }
        return metadataFieldConverter.convert(source)
    }
}

fun validatePhyloTreeField(
    source: String,
    fieldConverter: FieldConverter<PlainField>,
    databaseConfig: DatabaseConfig,
): PlainField {
    val converted = fieldConverter.convert(source)
    val validFields = databaseConfig.schema.metadata.filter { it.isPhyloTreeField }.map { it.name }
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
