package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.genspectrum.lapis.config.DatabaseConfig
import org.springframework.stereotype.Component
import java.util.stream.Stream

interface CsvRecord {
    @JsonIgnore
    fun getValuesList(comparator: Comparator<String>): List<String?>

    @JsonIgnore
    fun getHeader(comparator: Comparator<String>): List<String>
}

@Component
class CsvWriter(
    private val databaseConfig: DatabaseConfig,
) {
    fun write(
        appendable: Appendable,
        includeHeaders: Boolean,
        data: Stream<out CsvRecord>,
        delimiter: Delimiter,
        csvColumnOrder: CsvColumnOrder,
    ) {
        var shouldWriteHeaders = includeHeaders

        val columnComparator = getColumnComparator(csvColumnOrder)

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
                    it.printRecord(datum.getHeader(columnComparator))
                    shouldWriteHeaders = false
                }
                it.printRecord(datum.getValuesList(columnComparator))
            }
        }
    }

    private fun getColumnComparator(csvColumnOrder: CsvColumnOrder) =
        when (csvColumnOrder) {
            CsvColumnOrder.Undefined -> Comparator { _, _ -> 0 }
            CsvColumnOrder.AsInConfig -> SameOrderAsListComparator(databaseConfig.schema.metadata.map { it.name })
            is CsvColumnOrder.AsFieldsInRequest -> SameOrderAsListComparator(csvColumnOrder.fields)
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

    data object Undefined : CsvColumnOrder

    data class AsFieldsInRequest(
        val fields: List<String>,
    ) : CsvColumnOrder
}

private class SameOrderAsListComparator<T>(
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
