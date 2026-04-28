package org.genspectrum.lapis.silo

import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.BitVector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.ipc.ArrowStreamWriter
import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.Field
import org.apache.arrow.vector.types.pojo.FieldType
import org.apache.arrow.vector.types.pojo.Schema
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

/**
 * Builds an Arrow IPC stream byte array from [rows] using the given builder.
 *
 * Usage:
 * ```
 * buildArrowIpcStream(
 *     listOf(mapOf("count" to 6L, "division" to "Aargau"))
 * )
 * ```
 * Each row is a map of column name to value. All rows must have the same keys.
 * Supported value types: Long (int64), Int (int32), Double (float64), String, Boolean, null.
 */
fun buildArrowIpcStream(rows: List<Map<String, Any?>>): ByteArray {
    if (rows.isEmpty()) {
        return buildEmptyArrowIpcStream()
    }

    val sampleRow = rows.first()
    val allocator = RootAllocator()

    val fields = sampleRow.entries.map { (name, value) ->
        when (value) {
            is Long -> Field(name, FieldType.nullable(ArrowType.Int(64, true)), null)

            is Int -> Field(name, FieldType.nullable(ArrowType.Int(32, true)), null)

            is Double -> Field(
                name,
                FieldType.nullable(
                    ArrowType.FloatingPoint(org.apache.arrow.vector.types.FloatingPointPrecision.DOUBLE),
                ),
                null,
            )

            is Boolean -> Field(name, FieldType.nullable(ArrowType.Bool()), null)

            // Note: null values (and any unrecognized types) are typed as Utf8.
            // If the first row has null for a column that later rows fill with a typed value (e.g. Int, Long),
            // the column will be inferred as Utf8 and the converter will throw a cast error at runtime.
            // Avoid this in tests by ensuring the first row contains non-null values for all typed columns.
            else -> Field(name, FieldType.nullable(ArrowType.Utf8()), null)
        }
    }

    val schema = Schema(fields)
    val root = VectorSchemaRoot.create(schema, allocator)

    root.allocateNew()
    root.rowCount = rows.size

    rows.forEachIndexed { rowIdx, row ->
        sampleRow.keys.forEachIndexed { colIdx, colName ->
            val value = row[colName]
            when (val vector = root.getVector(colIdx)) {
                is BigIntVector -> {
                    if (value == null) {
                        vector.setNull(rowIdx)
                    } else {
                        vector.set(rowIdx, value as Long)
                    }
                }

                is IntVector -> {
                    if (value == null) {
                        vector.setNull(rowIdx)
                    } else {
                        vector.set(rowIdx, value as Int)
                    }
                }

                is Float8Vector -> {
                    if (value == null) {
                        vector.setNull(rowIdx)
                    } else {
                        vector.set(rowIdx, value as Double)
                    }
                }

                is BitVector -> {
                    if (value == null) {
                        vector.setNull(rowIdx)
                    } else {
                        vector.set(rowIdx, if (value as Boolean) 1 else 0)
                    }
                }

                is VarCharVector -> {
                    if (value == null) {
                        vector.setNull(rowIdx)
                    } else {
                        vector.setSafe(rowIdx, (value as String).toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val outputStream = ByteArrayOutputStream()
    val writer = ArrowStreamWriter(root, null, Channels.newChannel(outputStream))
    writer.start()
    writer.writeBatch()
    writer.end()
    writer.close()
    root.close()
    allocator.close()

    return outputStream.toByteArray()
}

private fun buildEmptyArrowIpcStream(): ByteArray {
    val allocator = RootAllocator()
    val schema = Schema(emptyList())
    val root = VectorSchemaRoot.create(schema, allocator)

    val outputStream = ByteArrayOutputStream()
    val writer = ArrowStreamWriter(root, null, Channels.newChannel(outputStream))
    writer.start()
    writer.end()
    writer.close()
    root.close()
    allocator.close()

    return outputStream.toByteArray()
}
