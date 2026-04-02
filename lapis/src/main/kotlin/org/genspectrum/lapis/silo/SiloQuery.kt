package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.BitVector
import org.apache.arrow.vector.Float4Vector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.COUNT_PROPERTY
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.PhyloSubtreeData
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.util.UNALIGNED_PREFIX
import java.time.LocalDate

data class SiloQuery<ResponseType>(
    val action: SiloAction<ResponseType>,
    val filterExpression: SiloFilterExpression,
)

/** Converts one row at [rowIndex] from an Arrow [VectorSchemaRoot] to a typed Kotlin object. */
typealias ArrowRowConverter<T> = (root: VectorSchemaRoot, rowIndex: Int) -> T

private fun VectorSchemaRoot.getString(name: String, rowIndex: Int): String? {
    val vector = getVector(name) as? VarCharVector ?: return null
    if (vector.isNull(rowIndex)) return null
    return String(vector.get(rowIndex))
}

private fun VectorSchemaRoot.getInt(name: String, rowIndex: Int): Int? {
    val vector = getVector(name) as? IntVector ?: return null
    if (vector.isNull(rowIndex)) return null
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getLong(name: String, rowIndex: Int): Long? {
    val vector = getVector(name) as? BigIntVector ?: return null
    if (vector.isNull(rowIndex)) return null
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getDouble(name: String, rowIndex: Int): Double? {
    val vector = getVector(name) as? Float8Vector ?: return null
    if (vector.isNull(rowIndex)) return null
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.fieldValueAsJsonNode(columnIndex: Int, rowIndex: Int): JsonNode {
    val vector = getFieldVectors()[columnIndex]
    if (vector.isNull(rowIndex)) return NullNode.instance
    return when (vector) {
        is VarCharVector -> TextNode(String(vector.get(rowIndex)))
        is IntVector -> IntNode(vector.get(rowIndex))
        is BigIntVector -> LongNode(vector.get(rowIndex))
        is Float8Vector -> DoubleNode(vector.get(rowIndex))
        is Float4Vector -> FloatNode(vector.get(rowIndex))
        is BitVector -> BooleanNode.valueOf(vector.get(rowIndex) != 0)
        else -> TextNode(vector.getObject(rowIndex)?.toString() ?: "")
    }
}

class AggregationDataTypeReference : TypeReference<AggregationData>()

class MutationDataTypeReference : TypeReference<MutationData>()

class AminoAcidMutationDataTypeReference : TypeReference<MutationData>()

class DetailsDataTypeReference : TypeReference<DetailsData>()

class InsertionDataTypeReference : TypeReference<InsertionData>()

class SequenceDataTypeReference : TypeReference<SequenceData>()

class MostCommonAncestorDataTypeReference : TypeReference<MostCommonAncestorData>()

class PhyloSubtreeDataTypeReference : TypeReference<PhyloSubtreeData>()

interface CommonActionFields {
    val orderByFields: List<OrderByField>
    val limit: Int?
    val offset: Int?
    val randomize: RandomizeConfig?
}

const val ORDER_BY_RANDOM_FIELD_NAME = "random"

sealed class SiloAction<ResponseType>(
    @JsonIgnore val typeReference: TypeReference<ResponseType>,
    @JsonIgnore val cacheable: Boolean,
    @JsonIgnore val arrowConverter: ArrowRowConverter<ResponseType>,
) : CommonActionFields {
    companion object {
        fun aggregated(
            groupByFields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<AggregationData> =
            AggregatedAction(
                groupByFields = groupByFields,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
            )

        fun mutations(
            minProportion: Double? = null,
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
            fields: List<String> = emptyList(),
        ): SiloAction<MutationData> =
            MutationsAction(
                minProportion = minProportion,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
                fields = fields,
            )

        fun aminoAcidMutations(
            minProportion: Double? = null,
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
            fields: List<String> = emptyList(),
        ): SiloAction<MutationData> =
            AminoAcidMutationsAction(
                minProportion = minProportion,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
                fields = fields,
            )

        fun details(
            fields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<DetailsData> =
            DetailsAction(
                fields = fields,
                orderByFields = getOrderByFieldsList(orderByFields),
                randomize = getRandomize(orderByFields),
                limit = limit,
                offset = offset,
            )

        fun mostRecentCommonAncestor(
            phyloTreeField: String,
            printNodesNotInTree: Boolean = false,
        ): SiloAction<MostCommonAncestorData> =
            MostRecentCommonAncestorAction(
                columnName = phyloTreeField,
                printNodesNotInTree = printNodesNotInTree,
            )

        fun phyloSubtree(
            phyloTreeField: String,
            printNodesNotInTree: Boolean = false,
        ): SiloAction<PhyloSubtreeData> =
            PhyloSubtreeAction(
                columnName = phyloTreeField,
                printNodesNotInTree = printNodesNotInTree,
            )

        fun nucleotideInsertions(
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            NucleotideInsertionsAction(
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun aminoAcidInsertions(
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            AminoAcidInsertionsAction(
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun genomicSequence(
            type: SequenceType,
            sequenceNames: List<String>,
            additionalFields: List<String> = emptyList(),
            orderByFields: OrderBySpec = OrderBySpec.EMPTY,
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<SequenceData> =
            SequenceAction(
                type = type,
                sequenceNames = sequenceNames,
                additionalFields = additionalFields,
                orderByFields = getOrderByFieldsList(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        private fun getRandomize(orderByFields: OrderBySpec): RandomizeConfig =
            when (orderByFields) {
                is OrderBySpec.ByFields -> RandomizeConfig.Disabled
                is OrderBySpec.Random ->
                    orderByFields.seed?.let { RandomizeConfig.WithSeed(it) }
                        ?: RandomizeConfig.Enabled
            }

        private fun getOrderByFieldsList(orderByFields: OrderBySpec): List<OrderByField> =
            when (orderByFields) {
                is OrderBySpec.ByFields -> orderByFields.fields
                is OrderBySpec.Random -> emptyList()
            }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AggregatedAction(
        val groupByFields: List<String>,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<AggregationData>(
        typeReference = AggregationDataTypeReference(),
        cacheable = true,
        arrowConverter = { root, rowIndex ->
            val count = root.getLong(COUNT_PROPERTY, rowIndex)?.toInt() ?: 0
            val fields = root.schema.fields
                .filter { it.name != COUNT_PROPERTY }
                .mapIndexed { colIdx, field ->
                    val actualColIdx = root.schema.fields.indexOfFirst { it.name == field.name }
                    field.name to root.fieldValueAsJsonNode(actualColIdx, rowIndex)
                }
                .toMap()
            AggregationData(count, fields)
        },
    ) {
        val type: String = "Aggregated"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class MutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val fields: List<String> = emptyList(),
    ) : SiloAction<MutationData>(
        typeReference = MutationDataTypeReference(),
        cacheable = true,
        arrowConverter = MUTATION_DATA_ARROW_CONVERTER,
    ) {
        val type: String = "Mutations"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AminoAcidMutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val fields: List<String> = emptyList(),
    ) : SiloAction<MutationData>(
        typeReference = AminoAcidMutationDataTypeReference(),
        cacheable = true,
        arrowConverter = MUTATION_DATA_ARROW_CONVERTER,
    ) {
        val type: String = "AminoAcidMutations"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class DetailsAction(
        val fields: List<String> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<DetailsData>(
        typeReference = DetailsDataTypeReference(),
        cacheable = false,
        arrowConverter = { root, rowIndex ->
            DetailsData(
                root.schema.fields.mapIndexed { colIdx, field ->
                    field.name to root.fieldValueAsJsonNode(colIdx, rowIndex)
                }.toMap(),
            )
        },
    ) {
        val type: String = "Details"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class NucleotideInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<InsertionData>(
        typeReference = InsertionDataTypeReference(),
        cacheable = true,
        arrowConverter = INSERTION_DATA_ARROW_CONVERTER,
    ) {
        val type: String = "Insertions"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class MostRecentCommonAncestorAction(
        val columnName: String,
        val printNodesNotInTree: Boolean? = false,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        override val randomize: RandomizeConfig? = null,
    ) : SiloAction<MostCommonAncestorData>(
        typeReference = MostCommonAncestorDataTypeReference(),
        cacheable = true,
        arrowConverter = { root, rowIndex ->
            MostCommonAncestorData(
                mrcaNode = root.getString("mrcaNode", rowIndex),
                missingNodeCount = root.getInt("missingNodeCount", rowIndex) ?: 0,
                missingFromTree = root.getString("missingFromTree", rowIndex),
            )
        },
    ) {
        val type: String = "MostRecentCommonAncestor"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class PhyloSubtreeAction(
        val columnName: String,
        val printNodesNotInTree: Boolean? = false,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        override val randomize: RandomizeConfig? = null,
    ) : SiloAction<PhyloSubtreeData>(
        typeReference = PhyloSubtreeDataTypeReference(),
        cacheable = true,
        arrowConverter = { root, rowIndex ->
            PhyloSubtreeData(
                subtreeNewick = root.getString("subtreeNewick", rowIndex) ?: "",
                missingNodeCount = root.getInt("missingNodeCount", rowIndex) ?: 0,
                missingFromTree = root.getString("missingFromTree", rowIndex),
            )
        },
    ) {
        val type: String = "PhyloSubtree"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AminoAcidInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<InsertionData>(
        typeReference = InsertionDataTypeReference(),
        cacheable = true,
        arrowConverter = INSERTION_DATA_ARROW_CONVERTER,
    ) {
        val type: String = "AminoAcidInsertions"
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class SequenceAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: RandomizeConfig? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: SequenceType,
        val sequenceNames: List<String>,
        val additionalFields: List<String> = emptyList(),
    ) : SiloAction<SequenceData>(
        typeReference = SequenceDataTypeReference(),
        cacheable = false,
        arrowConverter = { root, rowIndex ->
            SequenceData(
                root.schema.fields.mapIndexed { colIdx, field ->
                    field.name.removePrefix(UNALIGNED_PREFIX) to root.fieldValueAsJsonNode(colIdx, rowIndex)
                }.toMap(),
            )
        },
    )
}

private val MUTATION_DATA_ARROW_CONVERTER: ArrowRowConverter<MutationData> = { root, rowIndex ->
    MutationData(
        mutation = root.getString("mutation", rowIndex),
        count = root.getInt("count", rowIndex),
        proportion = root.getDouble("proportion", rowIndex),
        sequenceName = root.getString("sequenceName", rowIndex),
        mutationFrom = root.getString("mutationFrom", rowIndex),
        mutationTo = root.getString("mutationTo", rowIndex),
        position = root.getInt("position", rowIndex),
        coverage = root.getInt("coverage", rowIndex),
    )
}

private val INSERTION_DATA_ARROW_CONVERTER: ArrowRowConverter<InsertionData> = { root, rowIndex ->
    InsertionData(
        count = root.getInt("count", rowIndex) ?: 0,
        insertion = root.getString("insertion", rowIndex) ?: "",
        insertedSymbols = root.getString("insertedSymbols", rowIndex) ?: "",
        position = root.getInt("position", rowIndex) ?: 0,
        sequenceName = root.getString("sequenceName", rowIndex) ?: "",
    )
}

sealed class SiloFilterExpression(
    val type: String,
)

data class StringEquals(
    val column: String,
    val value: String?,
) : SiloFilterExpression("StringEquals")

data class BooleanEquals(
    val column: String,
    val value: Boolean?,
) : SiloFilterExpression("BooleanEquals")

data class LineageEquals(
    val column: String,
    val value: String?,
    val includeSublineages: Boolean,
) : SiloFilterExpression("Lineage")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NucleotideSymbolEquals(
    val sequenceName: String?,
    val position: Int,
    val symbol: String,
) : SiloFilterExpression("NucleotideEquals")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HasNucleotideMutation(
    val sequenceName: String?,
    val position: Int,
) : SiloFilterExpression("HasNucleotideMutation")

data class AminoAcidSymbolEquals(
    val sequenceName: String,
    val position: Int,
    val symbol: String,
) : SiloFilterExpression("AminoAcidEquals")

data class HasAminoAcidMutation(
    val sequenceName: String,
    val position: Int,
) : SiloFilterExpression("HasAminoAcidMutation")

data class DateBetween(
    val column: String,
    val from: LocalDate?,
    val to: LocalDate?,
) : SiloFilterExpression("DateBetween")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NucleotideInsertionContains(
    val position: Int,
    val value: String,
    val sequenceName: String?,
) : SiloFilterExpression("InsertionContains")

data class AminoAcidInsertionContains(
    val position: Int,
    val value: String,
    val sequenceName: String,
) : SiloFilterExpression(
        "AminoAcidInsertionContains",
    )

data object True : SiloFilterExpression("True")

data class And(
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("And") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())
}

data class Or(
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("Or") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())
}

data class Not(
    val child: SiloFilterExpression,
) : SiloFilterExpression("Not")

data class Maybe(
    val child: SiloFilterExpression,
) : SiloFilterExpression("Maybe")

data class NOf(
    val numberOfMatchers: Int,
    val matchExactly: Boolean,
    val children: List<SiloFilterExpression>,
) : SiloFilterExpression("N-Of")

data class IntEquals(
    val column: String,
    val value: Int?,
) : SiloFilterExpression("IntEquals")

data class IntBetween(
    val column: String,
    val from: Int?,
    val to: Int?,
) : SiloFilterExpression("IntBetween")

data class FloatEquals(
    val column: String,
    val value: Double?,
) : SiloFilterExpression("FloatEquals")

data class FloatBetween(
    val column: String,
    val from: Double?,
    val to: Double?,
) : SiloFilterExpression("FloatBetween")

data class StringSearch(
    val column: String,
    val searchExpression: String,
) : SiloFilterExpression("StringSearch")

data class PhyloDescendantOf(
    val column: String,
    val internalNode: String,
) : SiloFilterExpression("PhyloDescendantOf")

data class IsNull(
    val column: String,
) : SiloFilterExpression("IsNull")

data class IsNotNull(
    val column: String,
) : SiloFilterExpression("IsNotNull")

enum class SequenceType {
    @JsonProperty("Fasta")
    UNALIGNED,

    @JsonProperty("FastaAligned")
    ALIGNED,
}

@JsonSerialize(using = RandomizeConfigSerializer::class)
sealed class RandomizeConfig {
    data object Enabled : RandomizeConfig()

    data object Disabled : RandomizeConfig()

    data class WithSeed(
        val seed: Int,
    ) : RandomizeConfig()
}

class RandomizeConfigSerializer : JsonSerializer<RandomizeConfig>() {
    override fun serialize(
        value: RandomizeConfig,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        when (value) {
            is RandomizeConfig.Enabled -> gen.writeBoolean(true)
            is RandomizeConfig.Disabled -> gen.writeBoolean(false)
            is RandomizeConfig.WithSeed -> {
                gen.writeStartObject()
                gen.writeNumberField("seed", value.seed)
                gen.writeEndObject()
            }
        }
    }
}
