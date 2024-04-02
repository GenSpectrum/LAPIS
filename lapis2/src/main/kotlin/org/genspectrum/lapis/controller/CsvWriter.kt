package org.genspectrum.lapis.controller

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.io.StringWriter

interface CsvRecord {
    @JsonIgnore
    fun getValuesList(): List<String?>

    @JsonIgnore
    fun getHeader(): Array<String>
}

@Component
class CsvWriter {
    fun write(
        headers: Array<String>?,
        data: List<CsvRecord>,
        delimiter: Delimiter,
    ): String {
        val stringWriter = StringWriter()
        CSVPrinter(
            stringWriter,
            CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .setDelimiter(delimiter.value)
                .setNullString("")
                .also {
                    when {
                        headers != null -> it.setHeader(*headers)
                    }
                }
                .build(),
        ).use {
            for (datum in data) {
                it.printRecord(datum.getValuesList())
            }
        }
        return stringWriter.toString().trimEnd('\n')
    }
}

enum class Delimiter(val value: Char) {
    COMMA(','),
    TAB('\t'),
}
