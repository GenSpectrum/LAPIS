package org.genspectrum.lapis.silonew

import org.apache.arrow.vector.BigIntVector
import org.apache.arrow.vector.BitVector
import org.apache.arrow.vector.DateDayVector
import org.apache.arrow.vector.FieldVector
import org.apache.arrow.vector.Float4Vector
import org.apache.arrow.vector.Float8Vector
import org.apache.arrow.vector.IntVector
import org.apache.arrow.vector.VarCharVector
import tools.jackson.core.JsonGenerator
import java.io.OutputStream
import java.time.LocalDate

/** Type-specific access to one Arrow column. Instances can be reused while Arrow loads subsequent batches. */
internal sealed class ArrowColumn(
    protected val vector: FieldVector,
) {
    val name: String
        get() = vector.name

    fun isNull(rowIndex: Int): Boolean = vector.isNull(rowIndex)

    fun detachedValue(rowIndex: Int): Any? = if (isNull(rowIndex)) null else detachedNonNullValue(rowIndex)

    fun textValue(rowIndex: Int): String? = if (isNull(rowIndex)) null else nonNullTextValue(rowIndex)

    fun writeJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        if (isNull(rowIndex)) {
            generator.writeNull()
        } else {
            writeNonNullJsonValue(generator, rowIndex)
        }
    }

    protected abstract fun detachedNonNullValue(rowIndex: Int): Any

    protected abstract fun nonNullTextValue(rowIndex: Int): String

    protected abstract fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    )
}

internal class ArrowStringColumn(
    private val stringVector: VarCharVector,
) : ArrowColumn(stringVector) {
    override fun detachedNonNullValue(rowIndex: Int) = String(stringVector.get(rowIndex), Charsets.UTF_8)

    override fun nonNullTextValue(rowIndex: Int) = detachedNonNullValue(rowIndex)

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        stringVector.get(rowIndex).let { generator.writeUTF8String(it, 0, it.size) }
    }

    fun writeBytes(
        rowIndex: Int,
        output: OutputStream,
    ) {
        val startOffset = stringVector.getStartOffset(rowIndex)
        stringVector.dataBuffer.getBytes(
            startOffset.toLong(),
            output,
            stringVector.getEndOffset(rowIndex) - startOffset,
        )
    }
}

private class ArrowIntColumn(
    private val intVector: IntVector,
) : ArrowColumn(intVector) {
    override fun detachedNonNullValue(rowIndex: Int) = intVector.get(rowIndex)

    override fun nonNullTextValue(rowIndex: Int) = intVector.get(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeNumber(intVector.get(rowIndex))
    }
}

private class ArrowLongColumn(
    private val longVector: BigIntVector,
) : ArrowColumn(longVector) {
    override fun detachedNonNullValue(rowIndex: Int) = longVector.get(rowIndex)

    override fun nonNullTextValue(rowIndex: Int) = longVector.get(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeNumber(longVector.get(rowIndex))
    }
}

private class ArrowFloatColumn(
    private val floatVector: Float4Vector,
) : ArrowColumn(floatVector) {
    override fun detachedNonNullValue(rowIndex: Int) = floatVector.get(rowIndex)

    override fun nonNullTextValue(rowIndex: Int) = floatVector.get(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeNumber(floatVector.get(rowIndex))
    }
}

private class ArrowDoubleColumn(
    private val doubleVector: Float8Vector,
) : ArrowColumn(doubleVector) {
    override fun detachedNonNullValue(rowIndex: Int) = doubleVector.get(rowIndex)

    override fun nonNullTextValue(rowIndex: Int) = doubleVector.get(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeNumber(doubleVector.get(rowIndex))
    }
}

private class ArrowBooleanColumn(
    private val booleanVector: BitVector,
) : ArrowColumn(booleanVector) {
    override fun detachedNonNullValue(rowIndex: Int) = booleanVector.get(rowIndex) != 0

    override fun nonNullTextValue(rowIndex: Int) = detachedNonNullValue(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeBoolean(booleanVector.get(rowIndex) != 0)
    }
}

private class ArrowDateColumn(
    private val dateVector: DateDayVector,
) : ArrowColumn(dateVector) {
    override fun detachedNonNullValue(rowIndex: Int): LocalDate =
        LocalDate.ofEpochDay(dateVector.get(rowIndex).toLong())

    override fun nonNullTextValue(rowIndex: Int) = detachedNonNullValue(rowIndex).toString()

    override fun writeNonNullJsonValue(
        generator: JsonGenerator,
        rowIndex: Int,
    ) {
        generator.writeString(nonNullTextValue(rowIndex))
    }
}

internal fun FieldVector.toArrowColumn(unsupportedVector: (FieldVector) -> RuntimeException): ArrowColumn =
    when (this) {
        is VarCharVector -> ArrowStringColumn(this)
        is IntVector -> ArrowIntColumn(this)
        is BigIntVector -> ArrowLongColumn(this)
        is Float4Vector -> ArrowFloatColumn(this)
        is Float8Vector -> ArrowDoubleColumn(this)
        is BitVector -> ArrowBooleanColumn(this)
        is DateDayVector -> ArrowDateColumn(this)
        else -> throw unsupportedVector(this)
    }
