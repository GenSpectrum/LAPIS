package org.genspectrum.lapis.silonew

import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.BitVector
import org.apache.arrow.vector.DateDayVector
import org.apache.arrow.vector.Float4Vector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VarBinaryVector
import org.apache.arrow.vector.VarCharVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.ipc.ArrowStreamWriter
import org.apache.arrow.vector.types.DateUnit
import org.apache.arrow.vector.types.FloatingPointPrecision
import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.Field
import org.apache.arrow.vector.types.pojo.FieldType
import org.apache.arrow.vector.types.pojo.Schema
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.time.LocalDate

data class TestArrowRow(
    val text: String,
    val int32: Int,
    val int64: Long,
    val float32: Float,
    val float64: Double,
    val flag: Boolean,
    val date: LocalDate,
    val optional: String?,
)

fun buildTestArrowStream(batches: List<List<TestArrowRow>>): ByteArray {
    val fields = listOf(
        Field("text", FieldType.nullable(ArrowType.Utf8()), null),
        Field("int32", FieldType.nullable(ArrowType.Int(32, true)), null),
        Field("int64", FieldType.nullable(ArrowType.Int(64, true)), null),
        Field("float32", FieldType.nullable(ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null),
        Field("float64", FieldType.nullable(ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
        Field("flag", FieldType.nullable(ArrowType.Bool()), null),
        Field("date", FieldType.nullable(ArrowType.Date(DateUnit.DAY)), null),
        Field("optional", FieldType.nullable(ArrowType.Utf8()), null),
    )

    return writeArrowStream(schema = Schema(fields)) { root, writer ->
        batches.forEach { rows ->
            root.allocateNew()
            rows.forEachIndexed { index, row ->
                (root.getVector("text") as VarCharVector).setSafe(index, row.text.toByteArray())
                (root.getVector("int32") as IntVector).setSafe(index, row.int32)
                (root.getVector("int64") as BigIntVector).setSafe(index, row.int64)
                (root.getVector("float32") as Float4Vector).setSafe(index, row.float32)
                (root.getVector("float64") as Float8Vector).setSafe(index, row.float64)
                (root.getVector("flag") as BitVector).setSafe(index, if (row.flag) 1 else 0)
                (root.getVector("date") as DateDayVector).setSafe(index, row.date.toEpochDay().toInt())
                val optional = root.getVector("optional") as VarCharVector
                row.optional?.let { optional.setSafe(index, it.toByteArray()) } ?: optional.setNull(index)
            }
            root.rowCount = rows.size
            writer.writeBatch()
            root.clear()
        }
    }
}

fun buildUnsupportedArrowStream(): ByteArray {
    val schema = Schema(listOf(Field("binary", FieldType.nullable(ArrowType.Binary()), null)))
    return writeArrowStream(schema) { root, writer ->
        root.allocateNew()
        (root.getVector("binary") as VarBinaryVector).setSafe(0, byteArrayOf(1, 2, 3))
        root.rowCount = 1
        writer.writeBatch()
    }
}

fun buildTextArrowStream(
    fieldNames: List<String>,
    batches: List<List<Map<String, String?>>>,
): ByteArray {
    val schema = Schema(fieldNames.map { Field(it, FieldType.nullable(ArrowType.Utf8()), null) })
    return writeArrowStream(schema) { root, writer ->
        batches.forEach { rows ->
            root.allocateNew()
            rows.forEachIndexed { rowIndex, row ->
                fieldNames.forEach { fieldName ->
                    val vector = root.getVector(fieldName) as VarCharVector
                    row[fieldName]
                        ?.let { vector.setSafe(rowIndex, it.toByteArray(Charsets.UTF_8)) }
                        ?: vector.setNull(rowIndex)
                }
            }
            root.rowCount = rows.size
            writer.writeBatch()
            root.clear()
        }
    }
}

private fun writeArrowStream(
    schema: Schema,
    writeBatches: (VectorSchemaRoot, ArrowStreamWriter) -> Unit,
): ByteArray {
    val allocator = RootAllocator()
    val root = VectorSchemaRoot.create(schema, allocator)
    val output = ByteArrayOutputStream()
    try {
        ArrowStreamWriter(root, null, Channels.newChannel(output)).use { writer ->
            writer.start()
            writeBatches(root, writer)
            writer.end()
        }
        return output.toByteArray()
    } finally {
        root.close()
        allocator.close()
    }
}
