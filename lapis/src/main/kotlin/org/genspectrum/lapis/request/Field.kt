package org.genspectrum.lapis.request

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.stereotype.Component

private val SEQUENCE_POSITION_REGEX = Regex("""^([A-Za-z][A-Za-z0-9_]*)\[(\d+)\]$""")
private val SHORTHAND_POSITION_REGEX = Regex("""^\[(\d+)\]$""")

sealed interface RequestField {
    val outputColumnName: String
}

data class Field(
    val fieldName: String,
) : RequestField {
    override val outputColumnName: String get() = fieldName
}

data class SequencePositionField(
    val sequenceName: String,
    val position: Int,
) : RequestField {
    val columnAlias: String get() = "${sequenceName}_${position}"
    override val outputColumnName: String get() = columnAlias
}

fun interface FieldConverter<T> {
    fun convert(source: String): T

    fun validatePhyloTreeFields(source: String): T = convert(source)
}

@Component
class CaseInsensitiveFieldConverter(
    private val caseInsensitiveFieldsCleaner: CaseInsensitiveFieldsCleaner,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : FieldConverter<RequestField> {
    override fun convert(source: String): RequestField {
        val shorthandMatch = SHORTHAND_POSITION_REGEX.matchEntire(source)
        if (shorthandMatch != null) {
            val position = shorthandMatch.groupValues[1].toIntOrNull()
                ?: throw BadRequestException("Invalid position in '$source': must be a positive integer")
            if (position <= 0) {
                throw BadRequestException("Invalid position in '$source': must be a positive integer, got $position")
            }
            val canonicalName = referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName("main")
                ?: throw BadRequestException(
                    "Shorthand position syntax '[N]' requires a sequence named 'main' to exist",
                )
            return SequencePositionField(canonicalName, position)
        }

        val positionMatch = SEQUENCE_POSITION_REGEX.matchEntire(source)
        if (positionMatch != null) {
            val name = positionMatch.groupValues[1]
            val position = positionMatch.groupValues[2].toIntOrNull()
                ?: throw BadRequestException("Invalid position in '$source': must be a positive integer")
            if (position <= 0) {
                throw BadRequestException("Invalid position in '$source': must be a positive integer, got $position")
            }
            val canonicalName = referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(name)
                ?: throw BadRequestException(
                    "Unknown sequence '$name' in '$source', known sequences are: " +
                        (referenceGenomeSchema.getNucleotideSequenceNames() + referenceGenomeSchema.getGeneNames())
                            .joinToString(", "),
                )
            return SequencePositionField(canonicalName, position)
        }

        val cleaned = caseInsensitiveFieldsCleaner.clean(source)
            ?: throw BadRequestException(
                "Unknown field: '$source', known values are ${caseInsensitiveFieldsCleaner.getKnownFields()}",
            )
        return Field(cleaned)
    }

    override fun validatePhyloTreeFields(source: String): RequestField {
        val converted = convert(source)
        if (converted !is Field) {
            throw BadRequestException(
                "Position fields like '$source' cannot be used as phylo tree fields",
            )
        }
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
    fieldConverter: FieldConverter<RequestField>,
    databaseConfig: DatabaseConfig,
): Field {
    val converted = fieldConverter.convert(source)
    if (converted !is Field) {
        throw BadRequestException(
            "Position fields like '$source' cannot be used as phylo tree fields",
        )
    }
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
