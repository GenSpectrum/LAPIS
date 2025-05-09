package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class AminoAcidMutation(
    val gene: String,
    val position: Int,
    val symbol: String?,
    override val maybe: Boolean = false,
) : MaybeMutation<AminoAcidMutation> {
    companion object {
        fun fromString(
            aminoAcidMutation: String,
            referenceGenome: ReferenceGenomeSchema,
        ) = wrapWithMaybeMutationParser(aminoAcidMutation) { parseMutation(it, referenceGenome) }

        private fun parseMutation(
            aminoAcidMutation: String,
            referenceGenomeSchema: ReferenceGenomeSchema,
        ): AminoAcidMutation {
            val match = AMINO_ACID_MUTATION_REGEX.find(aminoAcidMutation)
                ?: throw BadRequestException("Invalid amino acid mutation: $aminoAcidMutation")

            val matchGroups = match.groups

            val gene = matchGroups["gene"]?.value
                ?: throw BadRequestException("Invalid amino acid mutation: $aminoAcidMutation: Did not find gene")
            val geneName = referenceGenomeSchema.getGene(gene)?.name
                ?: throw BadRequestException("Unknown gene: $gene")

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid amino acid mutation: $aminoAcidMutation: Did not find position",
                )

            return AminoAcidMutation(
                geneName,
                position,
                matchGroups["symbolTo"]?.value?.uppercase(),
            )
        }
    }

    override fun asMaybe() = copy(maybe = true)
}

private val AMINO_ACID_MUTATION_REGEX =
    Regex(
        """^((?<gene>[a-zA-Z0-9_-]+):)(?<symbolFrom>[a-zA-Z*]?)(?<position>\d+)(?<symbolTo>[a-zA-Z*.-])?$""",
    )

@JsonComponent
class AminoAcidMutationDeserializer(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : JsonDeserializer<AminoAcidMutation>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = AminoAcidMutation.fromString(p.valueAsString, referenceGenomeSchema)
}

@Component
class StringToAminoAcidMutationConverter(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) : Converter<String, AminoAcidMutation> {
    override fun convert(source: String) = AminoAcidMutation.fromString(source, referenceGenomeSchema)
}
