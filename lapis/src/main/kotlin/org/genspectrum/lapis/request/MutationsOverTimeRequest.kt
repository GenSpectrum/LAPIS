package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.springframework.boot.jackson.JsonComponent

data class MutationsOverTimeRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val includeMutations: List<NucleotideMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
) : BaseSequenceFilters

@JsonComponent
class MutationsOverTimeRequestDeserializer : JsonDeserializer<MutationsOverTimeRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MutationsOverTimeRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val parsedMutationsAndInsertions = parseMutationsAndInsertions(node, codec)

        val sequenceFilters = parseSequenceFilters(
            node,
            codec,
            SPECIAL_REQUEST_PROPERTIES + setOf("includeMutations", "dateRanges", "dateField"),
        )

        val includeMutations = when (val includeMutationsNode = node.get("includeMutations")) {
            null -> emptyList()
            is ArrayNode -> includeMutationsNode.map { codec.treeToValue(it, NucleotideMutation::class.java) }
            else -> throw BadRequestException(
                "includeMutations must be an array or null, ${butWas(includeMutationsNode)}",
            )
        }

        val dateRanges = when (val dateRangesNode = node.get("dateRanges")) {
            null -> emptyList()
            is ArrayNode -> dateRangesNode.map { codec.treeToValue(it, DateRange::class.java) }
            else -> throw BadRequestException(
                "dateRanges must be an array of objects {dateFrom: 'yyyy-mm-dd', dateTo: 'yyyy-mm-dd'} " +
                    "or null, ${butWas(dateRangesNode)}",
            )
        }

        return MutationsOverTimeRequest(
            sequenceFilters,
            parsedMutationsAndInsertions.nucleotideMutations,
            parsedMutationsAndInsertions.aminoAcidMutations,
            parsedMutationsAndInsertions.nucleotideInsertions,
            parsedMutationsAndInsertions.aminoAcidInsertions,
            includeMutations,
            dateRanges,
            node.get("dateField").asText(),
        )
    }
}
