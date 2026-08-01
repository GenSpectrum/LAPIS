package org.genspectrum.lapis.silonew

import org.apache.arrow.vector.VectorSchemaRoot
import java.time.LocalDate
import java.util.Collections

/**
 * A detached row returned by SILO.
 *
 * Values are represented as [String], [Int], [Long], [Float], [Double], [Boolean], [LocalDate], or `null`.
 * Typed getters return `null` for missing and null fields, and throw [SiloRowTypeException] for a different type.
 */
class SiloRow internal constructor(
    values: Map<String, Any?>,
) {
    val values: Map<String, Any?> = Collections.unmodifiableMap(values)

    val fieldNames: Set<String>
        get() = this.values.keys

    operator fun get(fieldName: String): Any? = values[fieldName]

    fun getString(fieldName: String): String? = getTyped(fieldName)

    fun getInt(fieldName: String): Int? = getTyped(fieldName)

    fun getLong(fieldName: String): Long? = getTyped(fieldName)

    fun getFloat(fieldName: String): Float? = getTyped(fieldName)

    fun getDouble(fieldName: String): Double? = getTyped(fieldName)

    fun getBoolean(fieldName: String): Boolean? = getTyped(fieldName)

    fun getDate(fieldName: String): LocalDate? = getTyped(fieldName)

    private inline fun <reified T : Any> getTyped(fieldName: String): T? {
        val value = values[fieldName] ?: return null
        if (value !is T) {
            throw SiloRowTypeException(
                fieldName = fieldName,
                expectedType = T::class.simpleName.orEmpty(),
                actualType = value::class.simpleName.orEmpty(),
            )
        }
        return value
    }

    override fun equals(other: Any?) = other is SiloRow && values == other.values

    override fun hashCode() = values.hashCode()

    override fun toString() = "SiloRow(values=$values)"
}

/** Indicates that a typed [SiloRow] getter was used for a field containing another type. */
class SiloRowTypeException internal constructor(
    fieldName: String,
    expectedType: String,
    actualType: String,
) : IllegalArgumentException(
        "Field '$fieldName' contains $actualType, but $expectedType was requested",
    )

internal class SiloRowMapper {
    private var root: VectorSchemaRoot? = null
    private lateinit var columns: List<ArrowColumn>

    fun map(
        root: VectorSchemaRoot,
        rowIndex: Int,
    ): SiloRow {
        if (this.root !== root) {
            this.root = root
            columns = root.fieldVectors.map { vector ->
                vector.toArrowColumn { unsupported ->
                    SiloResultDecodingException(
                        "Unsupported Arrow vector '${unsupported.javaClass.simpleName}' for field '${unsupported.name}'",
                    )
                }
            }
        }

        val values = LinkedHashMap<String, Any?>(columns.size)
        columns.forEach { column -> values[column.name] = column.detachedValue(rowIndex) }
        return SiloRow(values)
    }
}
