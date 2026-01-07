package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.springframework.boot.jackson.JsonComponent

data class NucleotideMutationsOverTimeRequest(
    val filters: QueriesOverTimeRequestFilters,
    val includeMutations: List<NucleotideMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
)

data class AminoAcidMutationsOverTimeRequest(
    val filters: QueriesOverTimeRequestFilters,
    val includeMutations: List<AminoAcidMutation>,
    val dateRanges: List<DateRange>,
    val dateField: String,
)

data class QueriesOverTimeRequest(
    val filters: QueriesOverTimeRequestFilters,
    val queries: List<QueryOverTimeItem>,
    val dateRanges: List<DateRange>,
    val dateField: String,
)

data class QueryOverTimeItem(
    val displayLabel: String?,
    val countQuery: String,
    val coverageQuery: String,
)

// TODO: fix schema
data class QueriesOverTimeRequestFilters(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
) : BaseSequenceFilters

@JsonComponent
class QueriesOverTimeRequestFiltersDeserializer : JsonDeserializer<QueriesOverTimeRequestFilters>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): QueriesOverTimeRequestFilters {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val parsedCommonFields = parseCommonFields(node, codec)

        return QueriesOverTimeRequestFilters(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
        )
    }
}
