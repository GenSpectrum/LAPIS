package org.genspectrum.lapis.controller

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.io.StringWriter

interface CsvRecord {
    fun asArray(): Array<String>

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
                .also {
                    when {
                        headers != null -> it.setHeader(*headers)
                    }
                }
                .build(),
        ).use {
            for (datum in data) {
                it.printRecord(*datum.asArray())
            }
        }
        return stringWriter.toString().trim()
    }
}

enum class Delimiter(val value: Char) {
    COMMA(','),
    TAB('\t'),
}
