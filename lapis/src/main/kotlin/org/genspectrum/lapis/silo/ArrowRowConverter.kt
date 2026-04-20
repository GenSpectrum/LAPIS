package org.genspectrum.lapis.silo

import com.fasterxml.jackson.databind.JsonNode
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
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.COUNT_PROPERTY
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MostCommonAncestorData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.PhyloSubtreeData
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.util.UNALIGNED_PREFIX

/** Converts one row at `rowIndex` from an Arrow [org.apache.arrow.vector.VectorSchemaRoot] to a typed Kotlin object. */
typealias ArrowRowConverter<T> = (root: VectorSchemaRoot, rowIndex: Int) -> T

val AGGREGATION_DATA_ARROW_CONVERTER: ArrowRowConverter<AggregationData> = { root, rowIndex ->
    val count = root.getLong(COUNT_PROPERTY, rowIndex)?.toInt() ?: 0
    val fields = root.schema.fields
        .filter { it.name != COUNT_PROPERTY }
        .mapIndexed { _, field ->
            val actualColIdx = root.schema.fields.indexOfFirst { it.name == field.name }
            field.name to root.fieldValueAsJsonNode(actualColIdx, rowIndex)
        }
        .toMap()
    AggregationData(count, fields)
}

val DETAILS_DATA_ARROW_CONVERTER: ArrowRowConverter<DetailsData> = { root, rowIndex ->
    DetailsData(
        root.schema.fields
            .mapIndexed { colIdx, field -> field.name to root.fieldValueAsJsonNode(colIdx, rowIndex) }
            .toMap(),
    )
}

val MOST_COMMON_ANCESTOR_DATA_ARROW_CONVERTER: ArrowRowConverter<MostCommonAncestorData> = { root, rowIndex ->
    MostCommonAncestorData(
        mrcaNode = root.getString("mrcaNode", rowIndex),
        missingNodeCount = root.getInt("missingNodeCount", rowIndex) ?: 0,
        missingFromTree = root.getString("missingFromTree", rowIndex),
    )
}

val MUTATION_DATA_ARROW_CONVERTER: ArrowRowConverter<MutationData> = { root, rowIndex ->
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

val INSERTION_DATA_ARROW_CONVERTER: ArrowRowConverter<InsertionData> = { root, rowIndex ->
    InsertionData(
        count = root.getInt("count", rowIndex) ?: 0,
        insertion = root.getString("insertion", rowIndex) ?: "",
        insertedSymbols = root.getString("insertedSymbols", rowIndex) ?: "",
        position = root.getInt("position", rowIndex) ?: 0,
        sequenceName = root.getString("sequenceName", rowIndex) ?: "",
    )
}

val PHYLO_SUBTREE_DATA_ARROW_CONVERTER: ArrowRowConverter<PhyloSubtreeData> = { root, rowIndex ->
    PhyloSubtreeData(
        subtreeNewick = root.getString("subtreeNewick", rowIndex) ?: "",
        missingNodeCount = root.getInt("missingNodeCount", rowIndex) ?: 0,
        missingFromTree = root.getString("missingFromTree", rowIndex),
    )
}

val SEQUENCE_DATA_ARROW_CONVERTER: ArrowRowConverter<SequenceData> = { root, rowIndex ->
    SequenceData(
        root.schema.fields
            .mapIndexed { colIdx, field ->
                field.name.removePrefix(UNALIGNED_PREFIX) to root.fieldValueAsJsonNode(colIdx, rowIndex)
            }
            .toMap(),
    )
}

private fun VectorSchemaRoot.getString(
    name: String,
    rowIndex: Int,
): String? {
    val vector = getVector(name) as? VarCharVector ?: return null
    if (vector.isNull(rowIndex)) {
        return null
    }
    return String(vector.get(rowIndex))
}

private fun VectorSchemaRoot.getInt(
    name: String,
    rowIndex: Int,
): Int? {
    val vector = getVector(name) as? IntVector ?: return null
    if (vector.isNull(rowIndex)) {
        return null
    }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getLong(
    name: String,
    rowIndex: Int,
): Long? {
    val vector = getVector(name) as? BigIntVector ?: return null
    if (vector.isNull(rowIndex)) {
        return null
    }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getDouble(
    name: String,
    rowIndex: Int,
): Double? {
    val vector = getVector(name) as? Float8Vector ?: return null
    if (vector.isNull(rowIndex)) {
        return null
    }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.fieldValueAsJsonNode(
    columnIndex: Int,
    rowIndex: Int,
): JsonNode {
    val vector = fieldVectors[columnIndex]
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
