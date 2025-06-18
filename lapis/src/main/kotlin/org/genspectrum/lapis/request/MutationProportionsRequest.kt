package org.genspectrum.lapis.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JsonComponent

const val DEFAULT_MIN_PROPORTION = 0.05

data class MutationProportionsRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<MutationsField>,
    val minProportion: Double? = null,
    override val orderByFields: List<OrderByField> = emptyList(),
    override val limit: Int? = null,
    override val offset: Int? = null,
) : CommonSequenceFilters {
    fun shouldResponseContainSequenceName() = fields.isEmpty() || fields.contains(MutationsField.SEQUENCE_NAME)
}

enum class MutationsField(
    val value: String,
) {
    MUTATION("mutation"),
    COUNT("count"),
    COVERAGE("coverage"),
    PROPORTION("proportion"),
    SEQUENCE_NAME("sequenceName"),
    MUTATION_FROM("mutationFrom"),
    MUTATION_TO("mutationTo"),
    POSITION("position"),
    ;

    companion object {
        fun fromString(value: String): MutationsField =
            entries.find { it.value == value }
                ?: throw BadRequestException(
                    "Invalid mutations field: $value. Known values are ${entries.joinToString { it.value }}",
                )
    }
}

@JsonComponent
class MutationProportionsRequestDeserializer : JsonDeserializer<MutationProportionsRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MutationProportionsRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()
        val codec = jsonParser.codec

        val minProportion = when (val minProportionNode = node.get(MIN_PROPORTION_PROPERTY)) {
            null, is NullNode -> DEFAULT_MIN_PROPORTION
            is NumericNode -> minProportionNode.doubleValue()

            else -> throw BadRequestException("minProportion must be a number, is $minProportionNode")
        }

        val fields = parseFieldsProperty(node) { MutationsField.fromString(it) }
        val parsedCommonFields = parseCommonFields(node, codec)

        return MutationProportionsRequest(
            sequenceFilters = parsedCommonFields.sequenceFilters,
            nucleotideMutations = parsedCommonFields.nucleotideMutations,
            aminoAcidMutations = parsedCommonFields.aminoAcidMutations,
            nucleotideInsertions = parsedCommonFields.nucleotideInsertions,
            aminoAcidInsertions = parsedCommonFields.aminoAcidInsertions,
            fields = fields,
            minProportion = minProportion,
            orderByFields = parsedCommonFields.orderByFields,
            limit = parsedCommonFields.limit,
            offset = parsedCommonFields.offset,
        )
    }
}
