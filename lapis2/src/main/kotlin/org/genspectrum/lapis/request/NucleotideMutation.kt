package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

data class NucleotideMutation(val sequenceName: String?, val position: Int, val symbol: String?) {
    companion object {
        fun fromString(nucleotideMutation: String): NucleotideMutation {
            val match = NUCLEOTIDE_MUTATION_REGEX.find(nucleotideMutation)
                ?: throw BadRequestException("Invalid nucleotide mutation: $nucleotideMutation")

            val matchGroups = match.groups

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid nucleotide mutation: $nucleotideMutation: Did not find position",
                )

            return NucleotideMutation(
                matchGroups["sequenceName"]?.value,
                position,
                matchGroups["symbolTo"]?.value,
            )
        }
    }
}

private val NUCLEOTIDE_MUTATION_REGEX =
    Regex(
        """^((?<sequenceName>[a-zA-Z0-9_-]+)(?=:):)?(?<symbolFrom>[A-Z]?)(?<position>\d+)(?<symbolTo>[A-Z.-])?$""",
    )

@JsonComponent
class NucleotideMutationDeserializer : JsonDeserializer<NucleotideMutation>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = NucleotideMutation.fromString(p.valueAsString)
}

@Component
class StringToNucleotideMutationConverter : Converter<String, NucleotideMutation> {
    override fun convert(source: String) = NucleotideMutation.fromString(source)
}
