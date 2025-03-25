package org.genspectrum.lapis.response

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
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
            for (csvRecord in data.getCsvRecords()) {
                it.printRecord(csvRecord)
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
