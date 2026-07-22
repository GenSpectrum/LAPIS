package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import org.springframework.stereotype.Component
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
    val fields: List<Field>,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters

/** Rejects sequence position fields, since the /details endpoint has no notion of a per-position column. */
@Component
class DetailsFieldConverter(
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
) : FieldConverter<Field> {
    override fun convert(source: String): Field {
        val converted = caseInsensitiveFieldConverter.convert(source)
        if (converted !is Field) {
            throw BadRequestException(
                "Sequence position fields are not supported for this endpoint: ${converted.outputColumnName}",
            )
        }
        return converted
    }
}

@JacksonComponent
class DetailsFiltersRequestDeserializer(
    private val detailsFieldConverter: DetailsFieldConverter,
) : ValueDeserializer<DetailsFiltersRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): DetailsFiltersRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val fields = parseFieldsProperty(node, detailsFieldConverter)
        val parsedCommonFields = parseCommonFields(node, ctxt)

        return DetailsFiltersRequest(
            parsedCommonFields.sequenceFilters,
            parsedCommonFields.nucleotideMutations,
            parsedCommonFields.aminoAcidMutations,
            parsedCommonFields.nucleotideInsertions,
            parsedCommonFields.aminoAcidInsertions,
            fields,
            parsedCommonFields.orderByFields,
            parsedCommonFields.limit,
            parsedCommonFields.offset,
        )
    }
}
