package org.genspectrum.lapis.request

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.model.mutationsOverTime.DateRange
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer

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
    @param:Schema(
        description = "A display label to be used in the result table. Defaults to the 'countQuery' if not provided.",
    )
    val displayLabel: String? = null,
    @param:Schema(
        description = "An advanced query to compute the count in each date range.",
    )
    val countQuery: String,
    @param:Schema(
        description = "An advanced query to compute the coverage in each date range.",
    )
    val coverageQuery: String,
)

data class QueriesOverTimeRequestFilters(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
) : BaseSequenceFilters

@JacksonComponent
class QueriesOverTimeRequestFiltersDeserializer : ValueDeserializer<QueriesOverTimeRequestFilters>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): QueriesOverTimeRequestFilters {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val parsedCommonFields = parseCommonFields(node, ctxt)

        return QueriesOverTimeRequestFilters(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
        )
    }
}
