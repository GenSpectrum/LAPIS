package org.genspectrum.lapis.silonew

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.Flushable

internal class DelimitedTextWriter(
    private val appendable: Appendable,
    delimiter: Char,
    private val escapeSpecialCharacters: Boolean,
    private val flushEachRecord: Boolean = false,
) {
    private val printer = if (escapeSpecialCharacters) {
        null
    } else {
        CSVPrinter(
            appendable,
            CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .setDelimiter(delimiter)
                .setNullString("")
                .get(),
        )
    }
    private val delimiter = delimiter
    private var firstCell = true

    fun beginRecord() {
        check(firstCell) { "The previous delimited record is not complete" }
    }

    fun writeCell(value: String?) {
        if (escapeSpecialCharacters) {
            if (!firstCell) appendable.append(delimiter)
            appendable.append(value.orEmpty().escapeForDelimitedText(delimiter))
            firstCell = false
        } else {
            printer!!.print(value)
        }
    }

    fun endRecord() {
        if (escapeSpecialCharacters) {
            appendable.append('\n')
            firstCell = true
            if (flushEachRecord && appendable is Flushable) appendable.flush()
        } else {
            printer!!.println()
        }
    }

    fun writeRecord(values: Iterable<String?>) {
        beginRecord()
        values.forEach(::writeCell)
        endRecord()
    }

    fun flush() {
        printer?.flush()
        if (printer == null && appendable is Flushable) appendable.flush()
    }
}

private fun String.escapeForDelimitedText(delimiter: Char) =
    replace("\n", "\\n")
        .let { value ->
            if (delimiter == '\t') value.replace("\t", "\\t") else value.replace("$delimiter", "\\$delimiter")
        }

internal class FastaRecordWriter(
    private val output: java.io.OutputStream,
) {
    fun beginRecord(header: String) {
        output.write('>'.code)
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write('\n'.code)
    }

    fun writeSequence(sequence: ByteArray) = output.write(sequence)

    fun endRecord() = output.write('\n'.code)

    fun writeRecord(
        header: String,
        sequence: ByteArray,
    ) {
        beginRecord(header)
        writeSequence(sequence)
        endRecord()
    }

    fun flush() = output.flush()
}
