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
    val count = root.getLong(COUNT_PROPERTY, rowIndex).toInt()
    val fields = root.schema.fields
        .mapIndexedNotNull { colIdx, field ->
            if (field.name == COUNT_PROPERTY) {
                null
            } else {
                field.name to root.fieldValueAsJsonNode(colIdx, rowIndex)
            }
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
        mrcaNode = root.getOptionalString("mrcaNode", rowIndex),
        missingNodeCount = root.getInt("missingNodeCount", rowIndex),
        missingFromTree = root.getOptionalString("missingFromTree", rowIndex),
    )
}

val MUTATION_DATA_ARROW_CONVERTER: ArrowRowConverter<MutationData> = { root, rowIndex ->
    MutationData(
        mutation = root.getOptionalString("mutation", rowIndex),
        count = root.getOptionalInt("count", rowIndex),
        proportion = root.getOptionalDouble("proportion", rowIndex),
        sequenceName = root.getOptionalString("sequenceName", rowIndex),
        mutationFrom = root.getOptionalString("mutationFrom", rowIndex),
        mutationTo = root.getOptionalString("mutationTo", rowIndex),
        position = root.getOptionalInt("position", rowIndex),
        coverage = root.getOptionalInt("coverage", rowIndex),
    )
}

val INSERTION_DATA_ARROW_CONVERTER: ArrowRowConverter<InsertionData> = { root, rowIndex ->
    InsertionData(
        count = root.getInt("count", rowIndex),
        insertion = root.getString("insertion", rowIndex),
        insertedSymbols = root.getString("insertedSymbols", rowIndex),
        position = root.getInt("position", rowIndex),
        sequenceName = root.getString("sequenceName", rowIndex),
    )
}

val PHYLO_SUBTREE_DATA_ARROW_CONVERTER: ArrowRowConverter<PhyloSubtreeData> = { root, rowIndex ->
    PhyloSubtreeData(
        subtreeNewick = root.getString("subtreeNewick", rowIndex),
        missingNodeCount = root.getInt("missingNodeCount", rowIndex),
        missingFromTree = root.getOptionalString("missingFromTree", rowIndex),
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

private fun VectorSchemaRoot.getOptionalString(
    name: String,
    rowIndex: Int,
): String? {
    val rawVector = getVector(name) ?: return null
    val vector = rawVector as? VarCharVector
        ?: error("Expected VarCharVector for column '$name' but got ${rawVector.javaClass.simpleName}")
    if (vector.isNull(rowIndex)) {
        return null
    }
    return String(vector.get(rowIndex), Charsets.UTF_8)
}

private fun VectorSchemaRoot.getString(
    name: String,
    rowIndex: Int,
): String {
    val vector = getVector(name) as? VarCharVector
        ?: error("Expected VarCharVector for column '$name' but got ${getVector(name)?.javaClass?.simpleName}")
    check(!vector.isNull(rowIndex)) { "Unexpected null value in non-nullable column '$name' at row $rowIndex" }
    return String(vector.get(rowIndex), Charsets.UTF_8)
}

private fun VectorSchemaRoot.getOptionalInt(
    name: String,
    rowIndex: Int,
): Int? {
    val rawVector = getVector(name) ?: return null
    val vector = rawVector as? IntVector
        ?: error("Expected IntVector for column '$name' but got ${rawVector.javaClass.simpleName}")
    if (vector.isNull(rowIndex)) {
        return null
    }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getInt(
    name: String,
    rowIndex: Int,
): Int {
    val vector = getVector(name) as? IntVector
        ?: error("Expected IntVector for column '$name' but got ${getVector(name)?.javaClass?.simpleName}")
    check(!vector.isNull(rowIndex)) { "Unexpected null value in non-nullable column '$name' at row $rowIndex" }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getLong(
    name: String,
    rowIndex: Int,
): Long {
    val vector = getVector(name) as? BigIntVector
        ?: error("Expected BigIntVector for column '$name' but got ${getVector(name)?.javaClass?.simpleName}")
    check(!vector.isNull(rowIndex)) { "Unexpected null value in non-nullable column '$name' at row $rowIndex" }
    return vector.get(rowIndex)
}

private fun VectorSchemaRoot.getOptionalDouble(
    name: String,
    rowIndex: Int,
): Double? {
    val rawVector = getVector(name) ?: return null
    val vector = rawVector as? Float8Vector
        ?: error("Expected Float8Vector for column '$name' but got ${rawVector.javaClass.simpleName}")
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
        is VarCharVector -> TextNode(String(vector.get(rowIndex), Charsets.UTF_8))
        is IntVector -> IntNode(vector.get(rowIndex))
        is BigIntVector -> LongNode(vector.get(rowIndex))
        is Float8Vector -> DoubleNode(vector.get(rowIndex))
        is Float4Vector -> FloatNode(vector.get(rowIndex))
        is BitVector -> BooleanNode.valueOf(vector.get(rowIndex) != 0)
        else -> TextNode(vector.getObject(rowIndex)?.toString() ?: "")
    }
}
