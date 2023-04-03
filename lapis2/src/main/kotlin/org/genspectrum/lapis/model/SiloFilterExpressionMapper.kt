package org.genspectrum.lapis.model

import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val DATE_FROM = "dateFrom"
private const val DATE_TO = "dateTo"

@Component
class SiloFilterExpressionMapper {
    fun map(sequenceFilters: Map<String, String>): SiloFilterExpression {
        if (sequenceFilters.isEmpty()) {
            return True
        }

        val (dateRangeFilters, genericSequenceFilters) = sequenceFilters.entries.partition {
            it.key in DATE_RANGE_FILTER_KEYS
        }

        val filterExpressions = genericSequenceFilters.map { mapToSiloFilter(it.key, it.value) }

        return when (val dateBetweenFilter = mapToDateBetweenFilter(dateRangeFilters)) {
            null -> And(filterExpressions)
            else -> {
                val filterExpressionsWithDateBetween = filterExpressions.toMutableList()
                filterExpressionsWithDateBetween.add(dateBetweenFilter)
                return And(filterExpressionsWithDateBetween)
            }
        }
    }

    private fun mapToDateBetweenFilter(dateRangeFilters: List<Map.Entry<String, String>>): DateBetween? {
        if (dateRangeFilters.isEmpty()) {
            return null
        }

        val dateRangeFiltersMap = dateRangeFilters.associate { it.key to it.value }
        try {
            return DateBetween(
                from = getAsDate(dateRangeFiltersMap, DATE_FROM),
                to = getAsDate(dateRangeFiltersMap, DATE_TO),
            )
        } catch (exception: DateTimeParseException) {
            throw IllegalArgumentException(exception.message, exception)
        }
    }

    private fun getAsDate(asMap: Map<String, String>, key: String) = try {
        asMap[key]?.let(LocalDate::parse)
    } catch (exception: DateTimeParseException) {
        throw IllegalArgumentException("$key '${asMap[key]}' is not a valid date: ${exception.message}", exception)
    }

    private fun mapToSiloFilter(key: String, value: String) = when (key) {
        "pangoLineage" -> mapToPangoLineageFilter(value)
        "nucleotideMutations" -> mapToNucleotideFilter(value)
        else -> StringEquals(key, value)
    }

    private fun mapToPangoLineageFilter(value: String) = when {
        value.endsWith(".*") -> PangoLineageEquals(value.substringBeforeLast(".*"), includeSublineages = true)
        value.endsWith('*') -> PangoLineageEquals(value.substringBeforeLast('*'), includeSublineages = true)
        value.endsWith('.') -> throw IllegalArgumentException(
            "Invalid pango lineage: $value must not end with a dot. Did you mean '$value*'?",
        )

        else -> PangoLineageEquals(value, includeSublineages = false)
    }

    private fun mapToNucleotideFilter(userInput: String): SiloFilterExpression {
        val mutations = userInput.split(",")

        return And(
            mutations.map { mutationExpression ->
                val match = NUCLEOTIDE_MUTATION_REGEX.find(mutationExpression)
                    ?: throw IllegalArgumentException("Invalid nucleotide mutation: $mutationExpression")

                val (_, position: String, symbolTo: String) = match.destructured

                NucleotideSymbolEquals(position.toInt(), symbolTo)
            },
        )
    }

    companion object {
        private val DATE_RANGE_FILTER_KEYS = arrayOf(
            DATE_FROM,
            DATE_TO,
        )

        private var NUCLEOTIDE_MUTATION_REGEX: Regex

        init {
            val validNucleotideSymbols = listOf("A", "C", "G", "T")
            val validMutationNucleotideSymbols =
                listOf("N", "-", "M", "R", "W", "S", "Y", "K", "V", "H", "D", "B", ".") +
                    validNucleotideSymbols
            val validSymbolsFrom = validNucleotideSymbols.joinToString { it }
            var validSymbolsTo = validMutationNucleotideSymbols.joinToString { it }
            validSymbolsTo = validSymbolsTo.replace(".", "\\.")
            validSymbolsTo = validSymbolsTo.replace("-", "\\-")

            NUCLEOTIDE_MUTATION_REGEX =
                Regex(
                    "(?<symbolFrom>^[$validSymbolsFrom]?)(?<position>\\d+)(?<symbolTo>[$validSymbolsTo]\$)",
                )
        }
    }
}
