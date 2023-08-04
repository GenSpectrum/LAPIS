package org.genspectrum.lapis.controller

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.genspectrum.lapis.silo.DetailsData
import org.springframework.stereotype.Component
import java.io.StringWriter

interface CsvRecord {
    fun asArray(): Array<String>
}

@Component
class CsvWriter {
    fun write(headers: Array<String>, data: List<CsvRecord>, delimiter: Delimiter): String {
        val stringWriter = StringWriter()
        CSVPrinter(
            stringWriter,
            CSVFormat.DEFAULT.builder()
                .setRecordSeparator("\n")
                .setDelimiter(delimiter.value)
                .setHeader(*headers)
                .build(),
        ).use {
            for (datum in data) {
                it.printRecord(*datum.asArray())
            }
        }
        return stringWriter.toString().trim()
    }
}

fun DetailsData.asCsvRecord() = JsonValuesCsvRecord(this.values)

data class JsonValuesCsvRecord(val values: Collection<JsonNode>) : CsvRecord {
    override fun asArray() = values.map { it.asText() }.toTypedArray()
}

enum class Delimiter(val value: Char) {
    COMMA(','),
    TAB('\t'),
}
