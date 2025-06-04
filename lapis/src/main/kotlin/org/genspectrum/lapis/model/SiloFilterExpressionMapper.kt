package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ADVANCED_QUERY_FIELD
import org.genspectrum.lapis.config.SequenceFilterField
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.config.VARIANT_QUERY_FIELD
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.BaseSequenceFilters
import org.genspectrum.lapis.request.MaybeMutation
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.BooleanEquals
import org.genspectrum.lapis.silo.DateBetween
import org.genspectrum.lapis.silo.FloatBetween
import org.genspectrum.lapis.silo.FloatEquals
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.IntBetween
import org.genspectrum.lapis.silo.IntEquals
import org.genspectrum.lapis.silo.LineageEquals
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
import org.genspectrum.lapis.silo.True
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

data class SequenceFilterValue(
    val type: SequenceFilterFieldType,
    val values: List<String?>,
    val originalKey: String,
)

typealias SequenceFilterFieldName = String

@Component
class SiloFilterExpressionMapper(
    private val allowedSequenceFilterFields: SequenceFilterFields,
    private val variantQueryFacade: VariantQueryFacade,
    private val advancedQueryFacade: AdvancedQueryFacade,
) {
    fun map(sequenceFilters: BaseSequenceFilters): SiloFilterExpression {
        if (sequenceFilters.isEmpty()) {
            return True
        }

        val allowedSequenceFiltersWithType = sequenceFilters
            .sequenceFilters
            .map { (key, values) ->
                val nullableField = allowedSequenceFilterFields.fields[key.lowercase(Locale.US)]
                val (filterExpressionId, type) = mapToFilterExpressionIdentifier(nullableField, key)
                filterExpressionId to SequenceFilterValue(type, values, key)
            }
            .groupBy({ it.first }, { it.second })

        crossValidateFilters(
            allowedSequenceFiltersWithType,
            sequenceFilters.nucleotideMutations,
            sequenceFilters.aaMutations,
        )

        val filterExpressions = allowedSequenceFiltersWithType.map { (key, values) ->
            val (siloColumnName, filter) = key
            when (filter) {
                Filter.StringEquals -> mapToStringEqualsFilters(siloColumnName, values)
                Filter.PangoLineage -> mapToPangoLineageFilter(siloColumnName, values)
                Filter.DateBetween -> mapToDateBetweenFilter(siloColumnName, values)
                Filter.VariantQuery -> mapToVariantQueryFilter(values)
                Filter.IntEquals -> mapToIntEqualsFilter(siloColumnName, values)
                Filter.IntBetween -> mapToIntBetweenFilter(siloColumnName, values)
                Filter.FloatEquals -> mapToFloatEqualsFilter(siloColumnName, values)
                Filter.FloatBetween -> mapToFloatBetweenFilter(siloColumnName, values)
                Filter.BooleanEquals -> mapToBooleanEqualsFilters(siloColumnName, values)
                Filter.StringSearch -> mapToStringSearchFilters(siloColumnName, values)
                Filter.AdvancedQuery -> mapToAdvancedQueryFilter(values)
            }
        }

        val nucleotideMutationExpressions = sequenceFilters.nucleotideMutations.map { toNucleotideMutationFilter(it) }
        val aminoAcidMutationExpressions = sequenceFilters.aaMutations.map { toAminoAcidMutationFilter(it) }
        val nucleotideInsertionExpressions =
            sequenceFilters.nucleotideInsertions.map { toNucleotideInsertionFilter(it) }
        val aminoAcidInsertionExpressions = sequenceFilters.aminoAcidInsertions.map { toAminoAcidInsertionFilter(it) }

        return And(
            filterExpressions +
                nucleotideMutationExpressions +
                aminoAcidMutationExpressions +
                nucleotideInsertionExpressions +
                aminoAcidInsertionExpressions,
        )
    }

    private fun mapToFilterExpressionIdentifier(
        field: SequenceFilterField?,
        key: SequenceFilterFieldName,
    ): Pair<Pair<SequenceFilterFieldName, Filter>, SequenceFilterFieldType> {
        val type = field?.type
        val filterExpressionId = when (type) {
            is SequenceFilterFieldType.DateFrom -> Pair(type.associatedField, Filter.DateBetween)
            is SequenceFilterFieldType.DateTo -> Pair(type.associatedField, Filter.DateBetween)
            SequenceFilterFieldType.Date -> Pair(field.name, Filter.DateBetween)
            SequenceFilterFieldType.Lineage -> Pair(field.name, Filter.PangoLineage)
            is SequenceFilterFieldType.StringSearch -> Pair(type.associatedField, Filter.StringSearch)
            SequenceFilterFieldType.String -> Pair(field.name, Filter.StringEquals)
            SequenceFilterFieldType.VariantQuery -> Pair(field.name, Filter.VariantQuery)
            SequenceFilterFieldType.Int -> Pair(field.name, Filter.IntEquals)
            is SequenceFilterFieldType.IntFrom -> Pair(type.associatedField, Filter.IntBetween)
            is SequenceFilterFieldType.IntTo -> Pair(type.associatedField, Filter.IntBetween)
            SequenceFilterFieldType.Float -> Pair(field.name, Filter.FloatEquals)
            is SequenceFilterFieldType.FloatFrom -> Pair(type.associatedField, Filter.FloatBetween)
            is SequenceFilterFieldType.FloatTo -> Pair(type.associatedField, Filter.FloatBetween)
            SequenceFilterFieldType.Boolean -> Pair(field.name, Filter.BooleanEquals)
            SequenceFilterFieldType.AdvancedQuery -> Pair(field.name, Filter.AdvancedQuery)

            null -> throw BadRequestException(
                "'$key' is not a valid sequence filter key. Valid keys are: " +
                    allowedSequenceFilterFields.fields.values.joinToString { it.name },
            )
        }
        return Pair(filterExpressionId, type)
    }

    private fun crossValidateFilters(
        allowedSequenceFiltersWithType: Map<Pair<SequenceFilterFieldName, Filter>, List<SequenceFilterValue>>,
        nucleotideMutations: List<NucleotideMutation>,
        aaMutations: List<AminoAcidMutation>,
    ) {
        val containsVariantQuery = allowedSequenceFiltersWithType.keys.any { it.second == Filter.VariantQuery }
        val containsAdvancedQuery = allowedSequenceFiltersWithType.keys.any { it.second == Filter.AdvancedQuery }
        val containsSimpleVariantQuery = allowedSequenceFiltersWithType.keys.any { it.second in variantQueryTypes } ||
            nucleotideMutations.isNotEmpty() ||
            aaMutations.isNotEmpty()

        if (containsVariantQuery && containsSimpleVariantQuery) {
            throw BadRequestException(
                "$VARIANT_QUERY_FIELD filter cannot be used with other variant filters such as: " +
                    variantQueryTypes.joinToString(", "),
            )
        }

        if (containsVariantQuery && containsAdvancedQuery) {
            throw BadRequestException(
                "$VARIANT_QUERY_FIELD filter cannot be used with $ADVANCED_QUERY_FIELD filter",
            )
        }

        val intEqualFilters = allowedSequenceFiltersWithType.keys.filter { it.second == Filter.IntEquals }
        for ((intEqualsColumnName, _) in intEqualFilters) {
            val intBetweenFilterForSameColumn = allowedSequenceFiltersWithType[
                Pair(intEqualsColumnName, Filter.IntBetween),
            ]

            if (intBetweenFilterForSameColumn != null) {
                throw BadRequestException(
                    "Cannot filter by exact int field '$intEqualsColumnName' " +
                        "and by int range field '${intBetweenFilterForSameColumn[0].originalKey}'.",
                )
            }
        }

        val floatEqualFilters = allowedSequenceFiltersWithType.keys.filter { it.second == Filter.FloatEquals }
        for ((floatEqualsColumnName, _) in floatEqualFilters) {
            val floatBetweenFilterForSameColumn = allowedSequenceFiltersWithType[
                Pair(floatEqualsColumnName, Filter.FloatBetween),
            ]

            if (floatBetweenFilterForSameColumn != null) {
                throw BadRequestException(
                    "Cannot filter by exact float field '$floatEqualsColumnName' " +
                        "and by float range field '${floatBetweenFilterForSameColumn[0].originalKey}'.",
                )
            }
        }

        val stringSearchFilters = allowedSequenceFiltersWithType.keys.filter { it.second == Filter.StringEquals }
        for ((stringSearchColumnName) in stringSearchFilters) {
            val stringEqualsFilterForSameColumn = allowedSequenceFiltersWithType[
                Pair(stringSearchColumnName, Filter.StringSearch),
            ]

            if (stringEqualsFilterForSameColumn != null) {
                throw BadRequestException(
                    "Cannot filter for string regex '${stringEqualsFilterForSameColumn[0].originalKey}' " +
                        "and string equals '$stringSearchColumnName' for the same field.",
                )
            }
        }
    }

    private fun mapToStringEqualsFilters(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ) = Or(values[0].values.map { StringEquals(siloColumnName, it) })

    private fun mapToBooleanEqualsFilters(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ) = Or(
        values[0].values.map {
            if (it.isNullOrBlank()) {
                return@map BooleanEquals(siloColumnName, null)
            }
            val value = try {
                it.lowercase().toBooleanStrict()
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("'$it' is not a valid boolean.")
            }
            BooleanEquals(siloColumnName, value)
        },
    )

    private fun mapToVariantQueryFilter(values: List<SequenceFilterValue>): SiloFilterExpression {
        if (values[0].values.size != 1) {
            throw BadRequestException(
                "$VARIANT_QUERY_FIELD must have exactly one value, found ${values[0].values.size} values.",
            )
        }

        val variantQuery = values[0].values.single()

        if (variantQuery.isNullOrBlank()) {
            throw BadRequestException("$VARIANT_QUERY_FIELD must not be empty, got '$variantQuery'")
        }

        return variantQueryFacade.map(variantQuery)
    }

    private fun mapToAdvancedQueryFilter(values: List<SequenceFilterValue>): SiloFilterExpression {
        if (values[0].values.size != 1) {
            throw BadRequestException(
                "$ADVANCED_QUERY_FIELD must have exactly one value, found ${values[0].values.size} values.",
            )
        }

        val advancedQuery = values[0].values.single()

        if (advancedQuery.isNullOrBlank()) {
            throw BadRequestException("$ADVANCED_QUERY_FIELD must not be empty, got '$advancedQuery'")
        }

        return advancedQueryFacade.map(advancedQuery)
    }

    private fun mapToDateBetweenFilter(
        siloColumnName: String,
        values: List<SequenceFilterValue>,
    ): DateBetween {
        val (exactDateFilters, dateRangeFilters) = values.partition { (fieldType, _) ->
            fieldType == SequenceFilterFieldType.Date
        }

        if (exactDateFilters.isNotEmpty() && dateRangeFilters.isNotEmpty()) {
            throw BadRequestException(
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
        val filter = dateRangeFilters.find { (type, _, _) -> type is T }
        return getAsDate(filter)
    }

    private fun getAsDate(sequenceFilterValue: SequenceFilterValue?): LocalDate? {
        val (_, values, originalKey) = sequenceFilterValue ?: return null
        val value = extractSingleFilterValue(values, originalKey)

        if (value.isNullOrBlank()) {
            return null
        }

        try {
            return LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw BadRequestException("$originalKey '$value' is not a valid date: ${exception.message}", exception)
        }
    }

    private fun mapToPangoLineageFilter(
        column: String,
        values: List<SequenceFilterValue>,
    ) = Or(
        values[0].values.map {
            when {
                it.isNullOrBlank() -> LineageEquals(column, null, includeSublineages = false)
                it.endsWith(".*") -> LineageEquals(column, it.substringBeforeLast(".*"), includeSublineages = true)
                it.endsWith('*') -> LineageEquals(column, it.substringBeforeLast('*'), includeSublineages = true)
                it.endsWith('.') -> throw BadRequestException(
                    "Invalid pango lineage: $it must not end with a dot. Did you mean '$it*'?",
                )

                else -> LineageEquals(column, it, includeSublineages = false)
            }
        },
    )

    private fun mapToIntEqualsFilter(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ): SiloFilterExpression {
        val value = extractSingleFilterValue(values[0])
        if (value.isNullOrBlank()) {
            return IntEquals(siloColumnName, null)
        }
        try {
            return IntEquals(siloColumnName, value.toInt())
        } catch (exception: NumberFormatException) {
            throw BadRequestException(
                "$siloColumnName '$value' is not a valid integer: ${exception.message}",
                exception,
            )
        }
    }

    private fun mapToFloatEqualsFilter(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ): SiloFilterExpression {
        val value = extractSingleFilterValue(values[0])
        if (value.isNullOrBlank()) {
            return FloatEquals(siloColumnName, null)
        }
        try {
            return FloatEquals(siloColumnName, value.toDouble())
        } catch (exception: NumberFormatException) {
            throw BadRequestException(
                "$siloColumnName '$value' is not a valid float: ${exception.message}",
                exception,
            )
        }
    }

    private fun mapToIntBetweenFilter(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ): SiloFilterExpression =
        IntBetween(
            siloColumnName,
            from = findIntOfFilterType<SequenceFilterFieldType.IntFrom>(values),
            to = findIntOfFilterType<SequenceFilterFieldType.IntTo>(values),
        )

    private inline fun <reified T : SequenceFilterFieldType> findIntOfFilterType(
        dateRangeFilters: List<SequenceFilterValue>,
    ): Int? {
        val (_, values, originalKey) = dateRangeFilters.find { (type, _, _) -> type is T } ?: return null
        val value = extractSingleFilterValue(values, originalKey)

        if (value.isNullOrBlank()) {
            return null
        }

        try {
            return value.toInt()
        } catch (exception: NumberFormatException) {
            throw BadRequestException(
                "$originalKey '$value' is not a valid integer: ${exception.message}",
                exception,
            )
        }
    }

    private fun mapToFloatBetweenFilter(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ): SiloFilterExpression =
        FloatBetween(
            siloColumnName,
            from = findFloatOfFilterType<SequenceFilterFieldType.FloatFrom>(values),
            to = findFloatOfFilterType<SequenceFilterFieldType.FloatTo>(values),
        )

    private fun mapToStringSearchFilters(
        siloColumnName: SequenceFilterFieldName,
        values: List<SequenceFilterValue>,
    ) = Or(values[0].values.map { StringSearch(siloColumnName, it) })

    private inline fun <reified T : SequenceFilterFieldType> findFloatOfFilterType(
        dateRangeFilters: List<SequenceFilterValue>,
    ): Double? {
        val (_, values, originalKey) = dateRangeFilters.find { (type, _, _) -> type is T } ?: return null
        val value = extractSingleFilterValue(values, originalKey)

        if (value.isNullOrBlank()) {
            return null
        }

        try {
            return value.toDouble()
        } catch (exception: NumberFormatException) {
            throw BadRequestException(
                "$originalKey '$value' is not a valid float: ${exception.message}",
                exception,
            )
        }
    }

    private fun toNucleotideMutationFilter(nucleotideMutation: NucleotideMutation) =
        wrapInMaybe(
            nucleotideMutation,
            when (nucleotideMutation.symbol) {
                null -> HasNucleotideMutation(nucleotideMutation.sequenceName, nucleotideMutation.position)
                else -> NucleotideSymbolEquals(
                    nucleotideMutation.sequenceName,
                    nucleotideMutation.position,
                    nucleotideMutation.symbol,
                )
            },
        )

    private fun toAminoAcidMutationFilter(aaMutation: AminoAcidMutation) =
        wrapInMaybe(
            aaMutation,
            when (aaMutation.symbol) {
                null -> HasAminoAcidMutation(aaMutation.gene, aaMutation.position)
                else -> AminoAcidSymbolEquals(
                    aaMutation.gene,
                    aaMutation.position,
                    aaMutation.symbol,
                )
            },
        )

    private fun wrapInMaybe(
        maybeMutation: MaybeMutation<*>,
        expression: SiloFilterExpression,
    ) = when (maybeMutation.maybe) {
        true -> Maybe(expression)
        false -> expression
    }

    private fun toNucleotideInsertionFilter(nucleotideInsertion: NucleotideInsertion): NucleotideInsertionContains =
        NucleotideInsertionContains(
            nucleotideInsertion.position,
            nucleotideInsertion.insertions,
            nucleotideInsertion.segment,
        )

    private fun toAminoAcidInsertionFilter(aminoAcidInsertion: AminoAcidInsertion): AminoAcidInsertionContains =
        AminoAcidInsertionContains(
            aminoAcidInsertion.position,
            aminoAcidInsertion.insertions,
            aminoAcidInsertion.gene,
        )

    private enum class Filter {
        StringEquals,
        PangoLineage,
        DateBetween,
        VariantQuery,
        AdvancedQuery,
        IntEquals,
        IntBetween,
        FloatEquals,
        FloatBetween,
        BooleanEquals,
        StringSearch,
    }

    private val variantQueryTypes = listOf(Filter.PangoLineage)

    private fun extractSingleFilterValue(value: SequenceFilterValue) =
        extractSingleFilterValue(value.values, value.originalKey)

    private fun extractSingleFilterValue(
        values: List<String?>,
        originalKey: String,
    ): String? {
        if (values.size > 1) {
            throw BadRequestException(
                "Expected exactly one value for '$originalKey' but got ${values.size} values.",
            )
        }
        return values[0]
    }
}
