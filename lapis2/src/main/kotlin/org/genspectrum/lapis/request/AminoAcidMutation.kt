package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class AminoAcidMutation(val gene: String, val position: Int, val symbol: String?) {
    companion object {
        fun fromString(
            aminoAcidMutation: String,
            referenceGenome: ReferenceGenome,
        ): AminoAcidMutation {
            val match = AMINO_ACID_MUTATION_REGEX.find(aminoAcidMutation)
                ?: throw BadRequestException("Invalid amino acid mutation: $aminoAcidMutation")

            val matchGroups = match.groups

            val geneLowerCase = matchGroups["gene"]?.value?.lowercase()
                ?: throw BadRequestException("Invalid amino acid mutation: $aminoAcidMutation: Did not find gene")
            val geneName = referenceGenome.getGeneFromLowercaseName(geneLowerCase).name

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
}

private val AMINO_ACID_MUTATION_REGEX =
    Regex(
        """^((?<gene>[a-zA-Z0-9_-]+):)(?<symbolFrom>[a-zA-Z]?)(?<position>\d+)(?<symbolTo>[a-zA-Z*.-])?$""",
    )

@JsonComponent
class AminoAcidMutationDeserializer(
    private val referenceGenome: ReferenceGenome,
) : JsonDeserializer<AminoAcidMutation>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = AminoAcidMutation.fromString(p.valueAsString, referenceGenome)
}

@Component
class StringToAminoAcidMutationConverter(
    private val referenceGenome: ReferenceGenome,
) : Converter<String, AminoAcidMutation> {
    override fun convert(source: String) = AminoAcidMutation.fromString(source, referenceGenome)
}
