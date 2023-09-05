package org.genspectrum.lapis.controller

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.StringWriter

interface CsvRecord {
    fun asArray(): Array<String>
    fun getHeader(): Array<String>
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

enum class Delimiter(val value: Char) {
    COMMA(','),
    TAB('\t'),
}

@Service
class CsvWriterService(private val csvWriter: CsvWriter) {
    fun <Request : CommonSequenceFilters> getResponseAsCsv(
        request: Request,
        delimiter: Delimiter,
        getResponse: (request: Request) -> List<CsvRecord>,
    ): String {
        val data = getResponse(request)

        if (data.isEmpty()) {
            return ""
        }

        val headers = data[0].getHeader()
        return csvWriter.write(headers, data, delimiter)
    }
}
