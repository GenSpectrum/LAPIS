package org.genspectrum.lapis.request

import org.genspectrum.lapis.request.converter.PlainFieldConverter
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer

data class DetailsFiltersRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<PlainField>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

@JacksonComponent
class DetailsFiltersRequestDeserializer(
    private val plainFieldConverter: PlainFieldConverter,
) : ValueDeserializer<DetailsFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): DetailsFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val fields = parseFieldsProperty(node, plainFieldConverter)
        val parsedCommonFields = parseCommonFields(node, ctxt)

        return DetailsFiltersRequest(
            sequenceFilters = parsedCommonFields.sequenceFilters,
            nucleotideMutations = parsedCommonFields.nucleotideMutations,
            aminoAcidMutations = parsedCommonFields.aminoAcidMutations,
            nucleotideInsertions = parsedCommonFields.nucleotideInsertions,
            aminoAcidInsertions = parsedCommonFields.aminoAcidInsertions,
            fields = fields,
            orderByFields = parsedCommonFields.orderByFields,
            limit = parsedCommonFields.limit,
            offset = parsedCommonFields.offset,
        )
    }
}
