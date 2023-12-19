package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

const val LAPIS_INSERTION_AMBIGUITY_SYMBOL = "?"
const val SILO_INSERTION_AMBIGUITY_SYMBOL = ".*"

data class NucleotideInsertion(val position: Int, val insertions: String, val segment: String?) {
    companion object {
        fun fromString(
            nucleotideInsertion: String,
            referenceGenome: ReferenceGenome,
        ): NucleotideInsertion {
            val match = NUCLEOTIDE_INSERTION_REGEX.find(nucleotideInsertion)
                ?: throw BadRequestException("Invalid nucleotide mutation: $nucleotideInsertion")

            val matchGroups = match.groups

            val position = matchGroups["position"]?.value?.toInt()
                ?: throw BadRequestException(
                    "Invalid nucleotide insertion: $nucleotideInsertion: Did not find position",
                )

            val insertions = matchGroups["insertions"]?.value?.replace(
                LAPIS_INSERTION_AMBIGUITY_SYMBOL,
                SILO_INSERTION_AMBIGUITY_SYMBOL,
            )?.uppercase()
                ?: throw BadRequestException(
                    "Invalid nucleotide insertion: $nucleotideInsertion: Did not find insertions",
                )

            val segmentName = matchGroups["segment"]?.value?.lowercase()
                ?.let { referenceGenome.getNucleotideSequenceFromLowercaseName(it).name }

            return NucleotideInsertion(
                position,
                insertions,
                segmentName,
            )
        }
    }
}

private val NUCLEOTIDE_INSERTION_REGEX =
    Regex(
        """^ins_((?<segment>[a-zA-Z0-9_-]+)(?=:):)?(?<position>\d+):(?<insertions>(([a-zA-Z?]|(\.\*))+))$""",
    )

@JsonComponent
class NucleotideInsertionDeserializer(private val referenceGenome: ReferenceGenome) :
    JsonDeserializer<NucleotideInsertion>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = NucleotideInsertion.fromString(p.valueAsString, referenceGenome)
}

@Component
class StringToNucleotideInsertionConverter(
    private val referenceGenome: ReferenceGenome,
) : Converter<String, NucleotideInsertion> {
    override fun convert(source: String) = NucleotideInsertion.fromString(source, referenceGenome)
}
