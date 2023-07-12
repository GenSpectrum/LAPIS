package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class AminoAcidMutation(val gene: String, val position: Int, val symbol: String?) {
    companion object {
        fun fromString(aminoAcidMutation: String): AminoAcidMutation {
            val match = AMINO_ACID_MUTATION_REGEX.find(aminoAcidMutation)
                ?: throw IllegalArgumentException("Invalid amino acid mutation: $aminoAcidMutation")

            val matchGroups = match.groups

            val gene = matchGroups["gene"]?.value
                ?: throw IllegalArgumentException("Invalid amino acid mutation: $aminoAcidMutation: Did not find gene")
            val position = matchGroups["position"]?.value?.toInt()
                ?: throw IllegalArgumentException(
                    "Invalid amino acid mutation: $aminoAcidMutation: Did not find position",
                )

            return AminoAcidMutation(
                gene,
                position,
                matchGroups["symbolTo"]?.value,
            )
        }
    }
}

private val AMINO_ACID_MUTATION_REGEX =
    Regex(
        """^((?<gene>[a-zA-Z0-9_-]+):)(?<symbolFrom>[A-Z]?)(?<position>\d+)(?<symbolTo>[A-Z.-])?$""",
    )

@JsonComponent
class AminoAcidMutationDeserializer : JsonDeserializer<AminoAcidMutation>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
        AminoAcidMutation.fromString(p.valueAsString)
}

@Component
class StringToAminoAcidMutationConverter : Converter<String, AminoAcidMutation> {
    override fun convert(source: String) = AminoAcidMutation.fromString(source)
}
