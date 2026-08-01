package org.genspectrum.lapis.response

import org.genspectrum.lapis.silonew.DelimitedTextWriter
import org.springframework.stereotype.Component
import java.util.stream.Stream

interface RecordCollection<T> {
    val records: Stream<T>

    fun getHeader(): List<String>

    fun getCsvRecords(): Stream<List<String?>> = records.map { mapToCsvValuesList(it) }

    /**
     * Csv values - must be in the same order as the header.
     */
    fun mapToCsvValuesList(value: T): List<String?>
}

@Component
class CsvWriter {
    fun write(
        appendable: Appendable,
        includeHeaders: Boolean,
        data: RecordCollection<*>,
        delimiter: Delimiter,
    ) {
        val writer = DelimitedTextWriter(
            appendable = appendable,
            delimiter = delimiter.value,
            escapeSpecialCharacters = false,
        )
        if (includeHeaders) writer.writeRecord(data.getHeader())
        streamAndLogDisconnect("CSV/TSV data") {
            data.getCsvRecords().use { csvRecordStream ->
                csvRecordStream.forEach(writer::writeRecord)
            }
        }
        writer.flush()
    }
}

/**
 * The IANA TSV Writer implements TSV output according to this spec:
 * https://www.iana.org/assignments/media-types/text/tab-separated-values
 *
 * It escapes Tabs and newlines in cell values (instead of quoting the cell content).
 * Rudimentary TSV parsing with simple splitting will not handle quoted cell values,
 * and thus can cause issues, this is why this implementation was added.
 *
 * The class is called TSV writer, but it's still possible to set other delimiters.
 *
 * Newlines will be escaped as \n, tabs as \t, and other delimiters as \<delimiter>.
 */
@Component
class IanaTsvWriter {
    fun write(
        appendable: Appendable,
        includeHeaders: Boolean,
        data: RecordCollection<*>,
        delimiter: Delimiter,
    ) {
        val writer = DelimitedTextWriter(
            appendable = appendable,
            delimiter = delimiter.value,
            escapeSpecialCharacters = true,
            flushEachRecord = true,
        )
        if (includeHeaders) writer.writeRecord(data.getHeader())
        streamAndLogDisconnect("Iana CSV/TSV data") {
            data.getCsvRecords().use { csvRecordStream ->
                csvRecordStream.forEach(writer::writeRecord)
            }
        }
        writer.flush()
    }
}

enum class Delimiter(
    val value: Char,
) {
    COMMA(','),
    TAB('\t'),
}
