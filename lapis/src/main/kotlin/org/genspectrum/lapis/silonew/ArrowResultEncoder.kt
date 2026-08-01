package org.genspectrum.lapis.silonew

import org.apache.arrow.vector.FieldVector
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.ipc.ArrowStreamReader
import tools.jackson.core.JsonGenerator
import tools.jackson.core.ObjectWriteContext
import tools.jackson.core.StreamWriteFeature
import tools.jackson.core.json.JsonFactory
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.Locale

private val JSON_FACTORY = JsonFactory.builder()
    .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
    .rootValueSeparator("")
    .build()

internal fun encodeArrowResult(
    reader: ArrowStreamReader,
    format: SiloDataFormat,
    output: OutputStream,
) {
    when (format) {
        SiloDataFormat.Json -> encodeJson(reader, output, ndjson = false)
        SiloDataFormat.Ndjson -> encodeJson(reader, output, ndjson = true)
        is SiloDataFormat.Csv -> encodeDelimited(
            reader = reader,
            output = output,
            delimiter = ',',
            includeHeader = format.includeHeader,
            escapeSpecialCharacters = false,
        )
        is SiloDataFormat.Tsv -> encodeDelimited(
            reader = reader,
            output = output,
            delimiter = '\t',
            includeHeader = format.includeHeader,
            escapeSpecialCharacters = format.escapeSpecialCharacters,
        )
        is SiloDataFormat.Fasta -> encodeFasta(reader, output, format)
        is SiloDataFormat.Newick -> encodeNewick(reader, output, format)
    }
}

private fun encodeJson(
    reader: ArrowStreamReader,
    output: OutputStream,
    ndjson: Boolean,
) {
    val root = reader.vectorSchemaRoot
    val columns = root.encodingColumns()
    JSON_FACTORY.createGenerator(ObjectWriteContext.empty(), output).use { generator ->
        if (!ndjson) generator.writeStartArray()
        reader.forEachRow { rowIndex ->
            writeJsonRow(generator, columns, rowIndex)
            if (ndjson) generator.writeRaw('\n')
        }
        if (!ndjson) generator.writeEndArray()
    }
}

private fun writeJsonRow(
    generator: JsonGenerator,
    columns: List<ArrowColumn>,
    rowIndex: Int,
) {
    generator.writeStartObject()
    columns.forEach { column ->
        generator.writeName(column.name)
        column.writeJsonValue(generator, rowIndex)
    }
    generator.writeEndObject()
}

private fun encodeDelimited(
    reader: ArrowStreamReader,
    output: OutputStream,
    delimiter: Char,
    includeHeader: Boolean,
    escapeSpecialCharacters: Boolean,
) {
    val root = reader.vectorSchemaRoot
    val columns = root.encodingColumns()
    val writer = DelimitedTextWriter(
        appendable = OutputStreamWriter(output, Charsets.UTF_8),
        delimiter = delimiter,
        escapeSpecialCharacters = escapeSpecialCharacters,
    )
    if (includeHeader) {
        writer.beginRecord()
        columns.forEach { writer.writeCell(it.name) }
        writer.endRecord()
    }
    reader.forEachRow { rowIndex ->
        writer.beginRecord()
        columns.forEach { writer.writeCell(it.textValue(rowIndex)) }
        writer.endRecord()
    }
    writer.flush()
}

private fun encodeFasta(
    reader: ArrowStreamReader,
    output: OutputStream,
    format: SiloDataFormat.Fasta,
) {
    val root = reader.vectorSchemaRoot
    val columns = root.encodingColumns()
    val sequenceColumns = format.sequenceFields.map { field -> columns.requireColumn(field.name) }
    sequenceColumns.forEach { column ->
        if (column !is ArrowStringColumn) {
            throw SiloResultEncodingException("FASTA sequence field '${column.name}' must contain strings")
        }
    }
    val template = FastaTemplate(format.headerTemplate, columns)
    val writer = FastaRecordWriter(output)

    reader.forEachRow { rowIndex ->
        sequenceColumns.forEach { column ->
            if (!column.isNull(rowIndex)) {
                writer.beginRecord(template.fill(rowIndex, column.name))
                (column as ArrowStringColumn).writeBytes(rowIndex, output)
                writer.endRecord()
            }
        }
    }
    writer.flush()
}

private fun encodeNewick(
    reader: ArrowStreamReader,
    output: OutputStream,
    format: SiloDataFormat.Newick,
) {
    val column = reader.vectorSchemaRoot.encodingColumns().requireColumn(format.field.name)
    if (column !is ArrowStringColumn) {
        throw SiloResultEncodingException("Newick field '${column.name}' must contain strings")
    }

    var valueCount = 0
    reader.forEachRow { rowIndex ->
        if (!column.isNull(rowIndex)) {
            valueCount++
            if (valueCount > 1) {
                throw SiloResultEncodingException("Newick output requires exactly one non-null value")
            }
            column.writeBytes(rowIndex, output)
        }
    }
    if (valueCount == 0) {
        throw SiloResultEncodingException("Newick output requires exactly one non-null value")
    }
    output.flush()
}

private inline fun ArrowStreamReader.forEachRow(action: (Int) -> Unit) {
    while (loadNextBatch()) {
        repeat(vectorSchemaRoot.rowCount, action)
    }
}

private fun List<ArrowColumn>.requireColumn(fieldName: String): ArrowColumn {
    val exactMatch = filter { it.name == fieldName }
    if (exactMatch.size == 1) return exactMatch.single()

    val caseInsensitiveMatches = filter { it.name.equals(fieldName, ignoreCase = true) }
    if (caseInsensitiveMatches.size == 1) return caseInsensitiveMatches.single()
    if (caseInsensitiveMatches.size > 1) {
        throw SiloResultEncodingException("Result field '$fieldName' is ambiguous when matched case-insensitively")
    }
    throw SiloResultEncodingException("Result does not contain field '$fieldName'")
}

private fun VectorSchemaRoot.encodingColumns() = fieldVectors.map { it.toArrowColumn(::unsupportedVector) }

private fun unsupportedVector(vector: FieldVector): SiloResultEncodingException =
    SiloResultEncodingException(
        "Unsupported Arrow vector '${vector.javaClass.simpleName}' for field '${vector.name}'",
    )

private class FastaTemplate(
    private val template: String,
    columns: List<ArrowColumn>,
) {
    private val replacements = PLACEHOLDER.findAll(template)
        .map { it.groupValues[1] }
        .distinctBy { it.lowercase(Locale.US) }
        .associateWith { placeholder ->
            when (placeholder.lowercase(Locale.US)) {
                ".segment", ".gene" -> null
                else -> columns.requireColumn(placeholder)
            }
        }

    fun fill(
        rowIndex: Int,
        sequenceFieldName: String,
    ): String {
        var result = template
        replacements.forEach { (placeholder, column) ->
            val replacement = column?.textValue(rowIndex).orEmpty().ifEmpty {
                if (column == null) sequenceFieldName else ""
            }
            result = result.replace("{$placeholder}", replacement, ignoreCase = true)
        }
        return result
    }

    private companion object {
        val PLACEHOLDER = Regex("""\{([^}]+)}""")
    }
}
