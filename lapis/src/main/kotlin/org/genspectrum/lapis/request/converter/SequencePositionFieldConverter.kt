package org.genspectrum.lapis.request.converter

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.SequencePositionField
import org.springframework.stereotype.Component

private val SEQUENCE_POSITION_REGEX = Regex("""^([A-Za-z][A-Za-z0-9_]*)?\[(\d+)]$""")

/**
 * Recognizes and resolves sequence-position syntax like `S[501]` or the single-segment shorthand `[501]`.
 * [tryConvert] returns `null` for strings that aren't position-field syntax at all, so callers can fall back
 * to plain field handling; it throws for strings that match the syntax but are otherwise invalid.
 */
@Component
class SequencePositionFieldConverter(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
    fun tryConvert(source: String): SequencePositionField? {
        val positionMatch = SEQUENCE_POSITION_REGEX.matchEntire(source) ?: return null

        val name = positionMatch.groupValues[1]
        val position = positionMatch.groupValues[2].toIntOrNull()
            ?: throw BadRequestException("Invalid position in '$source': must be a positive integer")
        if (position <= 0) {
            throw BadRequestException("Invalid position in '$source': must be a positive integer, got $position")
        }

        if (name.isEmpty()) {
            if (!referenceGenomeSchema.isSingleSegmented()) {
                throw BadRequestException(
                    "Shorthand position syntax '[N]' can only be used for single-segmented genomes",
                )
            }
            val canonicalName = referenceGenomeSchema.nucleotideSequences.first().name
            return SequencePositionField(canonicalName, position, isSingleSegment = true)
        }

        val canonicalName = referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(name)
            ?: throw BadRequestException(
                "Unknown sequence '$name' in '$source', known sequences are: " +
                    (referenceGenomeSchema.getNucleotideSequenceNames() + referenceGenomeSchema.getGeneNames())
                        .joinToString(", "),
            )
        return SequencePositionField(canonicalName, position)
    }
}
