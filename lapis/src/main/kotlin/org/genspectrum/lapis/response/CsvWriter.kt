package org.genspectrum.lapis.response

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.genspectrum.lapis.log
import org.springframework.stereotype.Component
import java.io.Flushable
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
        CSVPrinter(
            appendable,
            CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .setDelimiter(delimiter.value)
                .setNullString("")
                .get(),
        ).use {
            if (includeHeaders) {
                it.printRecord(data.getHeader())
            }
            try {
                data.getCsvRecords().use { csvRecordStream ->
                    csvRecordStream.forEach { csvRecord ->
                        it.printRecord(csvRecord)
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Error writing Iana CSV/TSV data" }
                throw e
            }
        }
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
        if (includeHeaders) {
            writeRow(appendable, data.getHeader(), delimiter)
        }
        data.getCsvRecords().use { csvRecordStream ->
            try {
                csvRecordStream.forEach { csvRecord ->
                    writeRow(appendable, csvRecord.map { it.orEmpty() }, delimiter)
                }
            } catch (e: Exception) {
                log.error(e) { "Error writing Iana CSV/TSV data" }
                throw e
            }
        }
    }

    private fun writeRow(
        appendable: Appendable,
        cells: List<String>,
        delimiter: Delimiter,
    ) {
        val row = getRow(cells, delimiter)
        appendable.appendLine(row)
        if (appendable is Flushable) {
            (appendable as Flushable).flush()
        }
    }

    private fun getRow(
        record: List<String>,
        delimiter: Delimiter,
    ): String =
        record.joinToString(separator = delimiter.value.toString()) { cell ->
            cell
                .replace("\n", "\\n")
                .let { v ->
                    if (delimiter.value == '\t') {
                        v.replace("\t", "\\t")
                    } else {
                        v.replace(delimiter.value.toString(), "\\${delimiter.value}")
                    }
                }
        }
}

enum class Delimiter(
    val value: Char,
) {
    COMMA(','),
    TAB('\t'),
}
