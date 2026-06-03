package org.genspectrum.lapis.request

import org.genspectrum.lapis.controller.BadRequestException
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.NumericNode

const val DEFAULT_MIN_PROPORTION = 0.05

data class MutationProportionsRequest(
    override val sequenceFilters: SequenceFilters,
    override val nucleotideMutations: List<NucleotideMutation>,
    override val aminoAcidMutations: List<AminoAcidMutation>,
    override val nucleotideInsertions: List<NucleotideInsertion>,
    override val aminoAcidInsertions: List<AminoAcidInsertion>,
    val fields: List<MutationsField>,
    val minProportion: Double? = null,
    override val orderByFields: OrderBySpec = OrderBySpec.EMPTY,
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

@JacksonComponent
class MutationProportionsRequestDeserializer : ValueDeserializer<MutationProportionsRequest>() {
    override fun deserialize(
        jsonParser: JsonParser,
        ctxt: DeserializationContext,
    ): MutationProportionsRequest {
        val node = jsonParser.readValueAsTree<JsonNode>()

        val minProportion = when (val minProportionNode = node.get(MIN_PROPORTION_PROPERTY)) {
            null, is NullNode -> DEFAULT_MIN_PROPORTION
            is NumericNode -> minProportionNode.doubleValue()

            else -> throw BadRequestException("minProportion must be a number, is $minProportionNode")
        }

        val fields = parseFieldsProperty(node) { MutationsField.fromString(it) }
        val parsedCommonFields = parseCommonFields(node, ctxt)

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
