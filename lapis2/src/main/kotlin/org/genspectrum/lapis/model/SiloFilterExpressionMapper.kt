package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
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

data class SequenceFilterValue(val type: SequenceFilterFieldType, val value: String, val originalKey: String)

typealias SequenceFilterFieldName = String

@Component
class SiloFilterExpressionMapper(
    private val allowedSequenceFilterFields: SequenceFilterFields,
    private val variantQueryFacade: VariantQueryFacade,
) {
    fun map(sequenceFilters: Map<SequenceFilterFieldName, String>): SiloFilterExpression {
        if (sequenceFilters.isEmpty()) {
            return True
        }

        val allowedSequenceFiltersWithType = sequenceFilters
            .map { (key, value) ->
                val nullableType = allowedSequenceFilterFields.fields[key]
                val (filterExpressionId, type) = mapToFilterExpressionIdentifier(nullableType, key)
                filterExpressionId to SequenceFilterValue(type, value, key)
            }
            .groupBy({ it.first }, { it.second })

        if (allowedSequenceFiltersWithType.keys.any { it.second == Filter.VariantQuery } &&
            allowedSequenceFiltersWithType.keys.any { it.second in variantQueryTypes }
        ) {
            throw IllegalArgumentException(
                "variantQuery cannot be used with other variant filters",
            )
        }

        val filterExpressions = allowedSequenceFiltersWithType.map { (key, values) ->
            val (siloColumnName, filter) = key
            when (filter) {
                Filter.StringEquals -> StringEquals(siloColumnName, values[0].value)
                Filter.PangoLineage -> mapToPangoLineageFilter(siloColumnName, values[0].value)
                Filter.DateBetween -> mapToDateBetweenFilter(siloColumnName, values)
                Filter.NucleotideSymbolEquals -> mapToNucleotideFilter(values[0].value)
                Filter.VariantQuery -> mapToVariantQueryFilter(values[0].value)
            }
        }

        return And(filterExpressions)
    }

    private fun mapToFilterExpressionIdentifier(
        type: SequenceFilterFieldType?,
        key: SequenceFilterFieldName,
    ): Pair<Pair<SequenceFilterFieldName, Filter>, SequenceFilterFieldType> {
        val filterExpressionId = when (type) {
            is SequenceFilterFieldType.DateFrom -> Pair(type.associatedField, Filter.DateBetween)
            is SequenceFilterFieldType.DateTo -> Pair(type.associatedField, Filter.DateBetween)
            SequenceFilterFieldType.Date -> Pair(key, Filter.DateBetween)
            SequenceFilterFieldType.PangoLineage -> Pair(key, Filter.PangoLineage)
            SequenceFilterFieldType.String -> Pair(key, Filter.StringEquals)
            SequenceFilterFieldType.MutationsList -> Pair(key, Filter.NucleotideSymbolEquals)
            SequenceFilterFieldType.VariantQuery -> Pair(key, Filter.VariantQuery)

            null -> throw IllegalArgumentException(
                "'$key' is not a valid sequence filter key. Valid keys are: " +
                    allowedSequenceFilterFields.fields.keys.joinToString(),
            )
        }
        return Pair(filterExpressionId, type)
    }

    private fun mapToVariantQueryFilter(variantQuery: String): SiloFilterExpression {
        if (variantQuery.isBlank()) {
            throw IllegalArgumentException(
                "variantQuery must not be empty",
            )
        }

        return variantQueryFacade.map(variantQuery)
    }

    private fun mapToDateBetweenFilter(
        siloColumnName: String,
        values: List<SequenceFilterValue>,
    ): DateBetween {
        val (exactDateFilters, dateRangeFilters) = values.partition { (fieldType, _) ->
            fieldType == SequenceFilterFieldType.Date
        }

        if (exactDateFilters.isNotEmpty() && dateRangeFilters.isNotEmpty()) {
            throw IllegalArgumentException(
                "Cannot filter by exact date field '${exactDateFilters[0].originalKey}' " +
                    "and by date range field '${dateRangeFilters[0].originalKey}'.",
            )
        }

        if (exactDateFilters.isNotEmpty()) {
            val date = getAsDate(exactDateFilters[0])
            return DateBetween(
                siloColumnName,
                from = date,
                to = date,
            )
        }

        return DateBetween(
            siloColumnName,
            from = findDateOfFilterType<SequenceFilterFieldType.DateFrom>(dateRangeFilters),
            to = findDateOfFilterType<SequenceFilterFieldType.DateTo>(dateRangeFilters),
        )
    }

    private inline fun <reified T : SequenceFilterFieldType> findDateOfFilterType(
        dateRangeFilters: List<SequenceFilterValue>,
    ): LocalDate? {
        val fromFilter = dateRangeFilters.find { (type, _, _) -> type is T }
        return getAsDate(fromFilter)
    }

    private fun getAsDate(sequenceFilterValue: SequenceFilterValue?): LocalDate? {
        val (_, value, originalKey) = sequenceFilterValue ?: return null

        try {
            return LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw IllegalArgumentException("$originalKey '$value' is not a valid date: ${exception.message}", exception)
        }
    }

    private fun mapToPangoLineageFilter(column: String, value: String) = when {
        value.endsWith(".*") -> PangoLineageEquals(column, value.substringBeforeLast(".*"), includeSublineages = true)
        value.endsWith('*') -> PangoLineageEquals(column, value.substringBeforeLast('*'), includeSublineages = true)
        value.endsWith('.') -> throw IllegalArgumentException(
            "Invalid pango lineage: $value must not end with a dot. Did you mean '$value*'?",
        )

        else -> PangoLineageEquals(column, value, includeSublineages = false)
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

    private enum class Filter {
        StringEquals,
        PangoLineage,
        DateBetween,
        NucleotideSymbolEquals,
        VariantQuery,
    }

    private val variantQueryTypes = listOf(Filter.PangoLineage, Filter.NucleotideSymbolEquals)
}
