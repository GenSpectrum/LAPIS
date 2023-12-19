package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class AminoAcidInsertion(val position: Int, val gene: String, val insertions: String) {
    companion object {
        fun fromString(
            aminoAcidInsertion: String,
            referenceGenome: ReferenceGenome,
        ): AminoAcidInsertion {
            val match = AMINO_ACID_INSERTION_REGEX.find(aminoAcidInsertion)
                ?: throw BadRequestException("Invalid nucleotide mutation: $aminoAcidInsertion")

            val matchGroups = match.groups

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find position",
                )

            val geneLowerCase = matchGroups["gene"]?.value?.lowercase()
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find gene",
                )
            val geneName = referenceGenome.getGeneFromLowercaseName(geneLowerCase).name

            val insertions = matchGroups["insertions"]?.value?.replace(
                LAPIS_INSERTION_AMBIGUITY_SYMBOL,
                SILO_INSERTION_AMBIGUITY_SYMBOL,
            )?.uppercase()
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find insertions",
                )

            return AminoAcidInsertion(
                position,
                geneName,
                insertions,
            )
        }
    }
}

private val AMINO_ACID_INSERTION_REGEX =
    Regex(
        """^ins_(?<gene>[a-zA-Z0-9_-]+):(?<position>\d+):(?<insertions>(([a-zA-Z?]|(\.\*))+))$""",
    )

@JsonComponent
class AminoAcidInsertionDeserializer(
    private val referenceGenome: ReferenceGenome,
) : JsonDeserializer<AminoAcidInsertion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = AminoAcidInsertion.fromString(p.valueAsString, referenceGenome)
}

@Component
class StringToAminoAcidInsertionConverter(
    private val referenceGenome: ReferenceGenome,
) : Converter<String, AminoAcidInsertion> {
    override fun convert(source: String) = AminoAcidInsertion.fromString(source, referenceGenome)
}
