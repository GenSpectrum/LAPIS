package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.util.stream.Stream

interface CsvRecord {
    @JsonIgnore
    fun getValuesList(): List<String?>

    @JsonIgnore
    fun getHeader(): Iterable<String>
}

@Component
class CsvWriter {
    fun write(
        appendable: Appendable,
        includeHeaders: Boolean,
        data: Stream<out CsvRecord>,
        delimiter: Delimiter,
    ) {
        var shouldWriteHeaders = includeHeaders

        CSVPrinter(
            appendable,
            CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .setDelimiter(delimiter.value)
                .setNullString("")
                .build(),
        ).use {
            for (datum in data) {
                if (shouldWriteHeaders) {
                    it.printRecord(datum.getHeader())
                    shouldWriteHeaders = false
                }
                it.printRecord(datum.getValuesList())
            }
        }
    }
}

enum class Delimiter(val value: Char) {
    COMMA(','),
    TAB('\t'),
}
