package org.genspectrum.lapis.response

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.stereotype.Component
import java.util.stream.Stream

interface CsvRecordCollection<T> {
    val records: Stream<T>

    fun getHeader(): List<String>

    fun getCsvRecords(): Stream<List<String?>> = records.map { mapToCsvValuesList(it) }

    fun mapToCsvValuesList(value: T): List<String?>
}

@Component
class CsvWriter(
    private val databaseConfig: DatabaseConfig,
) {
    fun write(
        appendable: Appendable,
        includeHeaders: Boolean,
        data: CsvRecordCollection<*>,
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

sealed interface CsvColumnOrder {
    data object AsInConfig : CsvColumnOrder

    data class AsFieldsInRequest(
        val fields: List<String>,
    ) : CsvColumnOrder
}

class SameOrderAsListComparator<T>(
    list: List<T>,
) : Comparator<T> {
    private val indexMap = list.withIndex().associate { it.value to it.index }

    override fun compare(
        o1: T,
        o2: T,
    ): Int {
        val index1 = indexMap[o1] ?: Int.MAX_VALUE
        val index2 = indexMap[o2] ?: Int.MAX_VALUE
        return index1.compareTo(index2)
    }
}
