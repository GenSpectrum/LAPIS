package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class AminoAcidInsertion(val position: Int, val gene: String, val insertions: String) {
    companion object {
        fun fromString(aminoAcidInsertion: String): AminoAcidInsertion {
            val match = AMINO_ACID_INSERTION_REGEX.find(aminoAcidInsertion)
                ?: throw BadRequestException("Invalid nucleotide mutation: $aminoAcidInsertion")

            val matchGroups = match.groups

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find position",
                )

            val gene = matchGroups["gene"]?.value
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find gene",
                )

            val insertions = matchGroups["insertions"]?.value?.replace("?", ".*")
                ?: throw BadRequestException(
                    "Invalid amino acid insertion: $aminoAcidInsertion: Did not find insertions",
                )

            return AminoAcidInsertion(
                position,
                gene,
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
class AminoAcidInsertionDeserializer : JsonDeserializer<AminoAcidInsertion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = AminoAcidInsertion.fromString(p.valueAsString)
}

@Component
class StringToAminoAcidInsertionConverter : Converter<String, AminoAcidInsertion> {
    override fun convert(source: String) = AminoAcidInsertion.fromString(source)
}
