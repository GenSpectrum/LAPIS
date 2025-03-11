package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class NucleotideMutation(
    val sequenceName: String?,
    val position: Int,
    val symbol: String?,
    override val maybe: Boolean = false,
) : MaybeMutation<NucleotideMutation> {
    companion object {
        fun fromString(
            nucleotideMutation: String,
            referenceGenome: ReferenceGenomeSchema,
        ) = wrapWithMaybeMutationParser(nucleotideMutation) { parseMutation(it, referenceGenome) }

        private fun parseMutation(
            nucleotideMutation: String,
            referenceGenomeSchema: ReferenceGenomeSchema,
        ): NucleotideMutation {
            val match = NUCLEOTIDE_MUTATION_REGEX.find(nucleotideMutation)
                ?: throw BadRequestException("Invalid nucleotide mutation: $nucleotideMutation")

            val matchGroups = match.groups

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid nucleotide mutation: $nucleotideMutation: Did not find position",
                )

            val segmentName = matchGroups["sequenceName"]?.value?.lowercase()
                ?.let { referenceGenomeSchema.getNucleotideSequenceFromLowercaseName(it).name }

            return NucleotideMutation(
                segmentName,
                position,
                matchGroups["symbolTo"]?.value?.uppercase(),
            )
        }
    }

    override fun asMaybe() = copy(maybe = true)
}

private val NUCLEOTIDE_MUTATION_REGEX =
    Regex(
        @Suppress("ktlint:standard:max-line-length")
        """^((?<sequenceName>[a-zA-Z0-9_-]+)(?=:):)?(?<symbolFrom>[a-zA-Z]?)(?<position>\d+)(?<symbolTo>[a-zA-Z.-])?$""",
    )

@JsonComponent
class NucleotideMutationDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : JsonDeserializer<NucleotideMutation>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = NucleotideMutation.fromString(p.valueAsString, referenceGenomeSchema)
}

@Component
class StringToNucleotideMutationConverter(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : Converter<String, NucleotideMutation> {
    override fun convert(source: String) = NucleotideMutation.fromString(source, referenceGenomeSchema)
}
