package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.response.CsvColumnOrder
import org.springframework.boot.jackson.JsonComponent

data class SequenceFiltersRequestWithFields(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aaMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<Field>,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters {
    override fun getCsvColumnOrder() =
        when (fields.isEmpty()) {
            true -> CsvColumnOrder.AsInConfig
            false -> CsvColumnOrder.AsFieldsInRequest(fields.map { it.fieldName })
        }
}

@JsonComponent
class SequenceFiltersRequestWithFieldsDeserializer(private val fieldConverter: FieldConverter) :
    JsonDeserializer<SequenceFiltersRequestWithFields>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): SequenceFiltersRequestWithFields {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val fields = when (val fields = node.get(FIELDS_PROPERTY)) {
            null -> emptyList()
            is ArrayNode -> fields.asSequence().map { fieldConverter.convert(it.asText()) }.toList()
            else -> throw BadRequestException(
                "fields must be an array or null",
            )
        }

        val parsedCommonFields = parseCommonFields(node, codec)

        return SequenceFiltersRequestWithFields(
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
