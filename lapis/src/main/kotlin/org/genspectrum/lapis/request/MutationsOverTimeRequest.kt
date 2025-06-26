package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.springframework.boot.jackson.JsonComponent

data class MutationsOverTimeRequest(
    val filters: MutationsOverTimeRequestFilters,
    val includeMutations: List<NucleotideMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
)

data class MutationsOverTimeRequestFilters(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
) : BaseSequenceFilters

@JsonComponent
class MutationsOverTimeRequestFiltersDeserializer : JsonDeserializer<MutationsOverTimeRequestFilters>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MutationsOverTimeRequestFilters {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val parsedCommonFields = parseCommonFields(node, codec)

        return MutationsOverTimeRequestFilters(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
        )
    }
}
