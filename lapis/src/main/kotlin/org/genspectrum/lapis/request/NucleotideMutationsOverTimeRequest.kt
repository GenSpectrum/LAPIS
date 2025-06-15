package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.springframework.boot.jackson.JsonComponent

data class NucleotideMutationsOverTimeRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val includeMutations: List<NucleotideMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
) : BaseSequenceFilters

data class AminoAcidMutationsOverTimeRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val includeMutations: List<AminoAcidMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
) : BaseSequenceFilters

@JsonComponent
class NucleotideMutationsOverTimeRequestDeserializer : JsonDeserializer<NucleotideMutationsOverTimeRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): NucleotideMutationsOverTimeRequest =
        deserializeRequest(
            jsonParser = jsonParser,
            includeMutationClass = NucleotideMutation::class.java,
            buildRequest = { filters, parsed, includes, ranges, field ->
                NucleotideMutationsOverTimeRequest(
                    filters,
                    parsed.nucleotideMutations,
                    parsed.aminoAcidMutations,
                    parsed.nucleotideInsertions,
                    parsed.aminoAcidInsertions,
                    includes,
                    ranges,
                    field,
                )
            },
        )
}

@JsonComponent
class AminoAcidMutationsOverTimeRequestDeserializer : JsonDeserializer<AminoAcidMutationsOverTimeRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): AminoAcidMutationsOverTimeRequest =
        deserializeRequest(
            jsonParser = jsonParser,
            includeMutationClass = AminoAcidMutation::class.java,
            buildRequest = { filters, parsed, includes, ranges, field ->
                AminoAcidMutationsOverTimeRequest(
                    filters,
                    parsed.nucleotideMutations,
                    parsed.aminoAcidMutations,
                    parsed.nucleotideInsertions,
                    parsed.aminoAcidInsertions,
                    includes,
                    ranges,
                    field,
                )
            },
        )
}

private fun <T, S> deserializeRequest(
    jsonParser: JsonParser,
    includeMutationClass: Class<T>,
    buildRequest: (
        sequenceFilters: SequenceFilters,
        parsed: ParsedMutationsAndInsertions,
        includeMutations: List<T>,
        dateRanges: List<DateRange>,
        dateField: String,
    ) -> S,
): S {
    val node = jsonParser.readValueAsTree<JsonNode>()
    val codec = jsonParser.codec

    val parsedMutationsAndInsertions = parseMutationsAndInsertions(node, codec)

    val sequenceFilters = parseSequenceFilters(
        node,
        codec,
        SPECIAL_REQUEST_PROPERTIES + setOf("includeMutations", "dateRanges", "dateField"),
    )

    val includeMutations = when (val includeNode = node.get("includeMutations")) {
        null -> emptyList()
        is ArrayNode -> includeNode.map { codec.treeToValue(it, includeMutationClass) }
        else -> throw BadRequestException(
            "includeMutations must be an array or null, ${butWas(includeNode)}",
        )
    }

    val dateRanges = when (val dateRangesNode = node.get("dateRanges")) {
        null -> emptyList()
        is ArrayNode -> dateRangesNode.map { codec.treeToValue(it, DateRange::class.java) }
        else -> throw BadRequestException(
            "dateRanges must be an array of objects {dateFrom: 'yyyy-mm-dd', dateTo: 'yyyy-mm-dd'} or null, ${butWas(
                dateRangesNode,
            )}",
        )
    }

    val dateField = node.get("dateField")?.asText()
        ?: throw BadRequestException("Missing required field: dateField")

    return buildRequest(sequenceFilters, parsedMutationsAndInsertions, includeMutations, dateRanges, dateField)
}
