package org.genspectrum.lapis.silo

import org.genspectrum.lapis.request.Order
import org.genspectrum.lapis.request.OrderByField
import java.time.LocalDate

object SiloQuerySaneQlSerializer {
    fun serialize(query: SiloQuery<*>): String {
        val filter = serializeFilter(query.filterExpression)
        val action = serializeAction(query.action)
        val suffix = serializePipelineSuffix(query.action)

        return buildString {
            append("default")
            if (filter != null) {
                append(".filter(")
                append(filter)
                append(")")
            }
            append(action)
            append(suffix)
        }
    }

    @Suppress("CyclomaticComplexity")
    private fun serializeFilter(expression: SiloFilterExpression): String? =
        when (expression) {
            is True -> null
            is StringEquals -> serializeStringEquals(expression)
            is BooleanEquals -> serializeBooleanEquals(expression)
            is IntEquals -> serializeIntEquals(expression)
            is FloatEquals -> serializeFloatEquals(expression)
            is DateBetween -> serializeDateBetween(expression)
            is IntBetween -> serializeIntBetween(expression)
            is FloatBetween -> serializeFloatBetween(expression)
            is LineageEquals -> serializeLineageEquals(expression)
            is StringSearch -> serializeStringSearch(expression)
            is IsNull -> "isNull(${expression.column})"
            is IsNotNull -> "isNotNull(${expression.column})"
            is NucleotideSymbolEquals -> serializeNucleotideSymbolEquals(expression)
            is HasNucleotideMutation -> serializeHasNucleotideMutation(expression)
            is AminoAcidSymbolEquals -> serializeAminoAcidSymbolEquals(expression)
            is HasAminoAcidMutation -> serializeHasAminoAcidMutation(expression)
            is NucleotideInsertionContains -> serializeNucleotideInsertionContains(expression)
            is AminoAcidInsertionContains -> serializeAminoAcidInsertionContains(expression)
            is PhyloDescendantOf -> serializePhyloDescendantOf(expression)
            is And -> expression.children.joinToString(" && ") { wrapIfNeeded(it) }
            is Or -> expression.children.joinToString(" || ") { wrapIfNeeded(it) }
            is Not -> "!(${serializeFilter(expression.child)})"
            is Maybe -> "maybe(${serializeFilter(expression.child)})"
            is NOf -> serializeNOf(expression)
        }

    private fun serializeStringEquals(expr: StringEquals): String =
        if (expr.value == null) {
            "isNull(${expr.column})"
        } else {
            "${expr.column} = ${str(expr.value)}"
        }

    private fun serializeBooleanEquals(expr: BooleanEquals): String =
        if (expr.value == null) {
            "isNull(${expr.column})"
        } else {
            "${expr.column} = ${expr.value}"
        }

    private fun serializeIntEquals(expr: IntEquals): String =
        if (expr.value == null) {
            "isNull(${expr.column})"
        } else {
            "${expr.column} = ${expr.value}"
        }

    private fun serializeFloatEquals(expr: FloatEquals): String =
        if (expr.value == null) {
            "isNull(${expr.column})"
        } else {
            "${expr.column} = ${expr.value}"
        }

    private fun serializeDateBetween(expr: DateBetween): String {
        val from = dateLiteral(expr.from)
        val to = dateLiteral(expr.to)
        return "${expr.column}.between($from, $to)"
    }

    private fun serializeIntBetween(expr: IntBetween): String {
        val from = expr.from ?: "null"
        val to = expr.to ?: "null"
        return "${expr.column}.between($from, $to)"
    }

    private fun serializeFloatBetween(expr: FloatBetween): String {
        val from = expr.from ?: "null"
        val to = expr.to ?: "null"
        return "${expr.column}.between($from, $to)"
    }

    private fun serializeLineageEquals(expr: LineageEquals): String {
        val value = if (expr.value == null) "null" else str(expr.value)
        return "${expr.column}.lineage($value, includeSublineages:=${expr.includeSublineages})"
    }

    private fun serializeStringSearch(expr: StringSearch): String {
        val pattern = str(expr.searchExpression)
        return "${expr.column}.like($pattern)"
    }

    private fun serializeNucleotideSymbolEquals(expr: NucleotideSymbolEquals) =
        buildString {
            append("nucleotideEquals(position:=${expr.position}, symbol:=${str(expr.symbol)}")
            if (expr.sequenceName != null) {
                append(", sequenceName:=${str(expr.sequenceName)}")
            }
            append(")")
        }

    private fun serializeHasNucleotideMutation(expr: HasNucleotideMutation) =
        buildString {
            append("hasMutation(position:=${expr.position}")
            if (expr.sequenceName != null) {
                append(", sequenceName:=${str(expr.sequenceName)}")
            }
            append(")")
        }

    private fun serializeAminoAcidSymbolEquals(expr: AminoAcidSymbolEquals): String {
        val sym = str(expr.symbol)
        val seq = str(expr.sequenceName)
        return "aminoAcidEquals(position:=${expr.position}, symbol:=$sym, sequenceName:=$seq)"
    }

    private fun serializeHasAminoAcidMutation(expr: HasAminoAcidMutation): String {
        val seq = str(expr.sequenceName)
        return "hasAAMutation(position:=${expr.position}, sequenceName:=$seq)"
    }

    private fun serializeNucleotideInsertionContains(expr: NucleotideInsertionContains) =
        buildString {
            append("insertionContains(position:=${expr.position}, value:=${str(expr.value)}")
            if (expr.sequenceName != null) {
                append(", sequenceName:=${str(expr.sequenceName)}")
            }
            append(")")
        }

    private fun serializeAminoAcidInsertionContains(expr: AminoAcidInsertionContains): String {
        val value = str(expr.value)
        val seq = str(expr.sequenceName)
        return "aminoAcidInsertionContains(position:=${expr.position}, value:=$value, sequenceName:=$seq)"
    }

    private fun serializePhyloDescendantOf(expr: PhyloDescendantOf): String {
        val node = str(expr.internalNode)
        return "${expr.column}.phyloDescendantOf($node)"
    }

    private fun serializeNOf(expr: NOf) =
        buildString {
            val children = expr.children.joinToString(", ") { serializeFilter(it)!! }
            append("nOf(${expr.numberOfMatchers}, {$children}")
            if (expr.matchExactly) {
                append(", matchExactly:=true")
            }
            append(")")
        }

    private fun serializeAction(action: SiloAction<*>): String =
        when (action) {
            is SiloAction.AggregatedAction -> serializeAggregatedAction(action)
            is SiloAction.DetailsAction -> serializeDetailsAction(action)
            is SiloAction.MutationsAction -> serializeMutationsAction(action)
            is SiloAction.AminoAcidMutationsAction -> serializeAminoAcidMutationsAction(action)
            is SiloAction.NucleotideInsertionsAction -> ".insertions()"
            is SiloAction.AminoAcidInsertionsAction -> ".aminoAcidInsertions()"
            is SiloAction.SequenceAction -> serializeSequenceAction(action)
            is SiloAction.MostRecentCommonAncestorAction -> serializeMRCAAction(action)
            is SiloAction.PhyloSubtreeAction -> serializePhyloSubtreeAction(action)
        }

    private fun serializeAggregatedAction(action: SiloAction.AggregatedAction) =
        buildString {
            append(".groupBy({count:=count()}")
            if (action.groupByFields.isNotEmpty()) {
                val fields = action.groupByFields.joinToString(", ")
                append(", {$fields}")
            }
            append(")")
        }

    private fun serializeDetailsAction(action: SiloAction.DetailsAction): String =
        if (action.fields.isEmpty()) {
            ""
        } else {
            val fields = action.fields.joinToString(", ")
            ".project({$fields})"
        }

    private fun serializeMutationsAction(action: SiloAction.MutationsAction) =
        buildString {
            append(".mutations(")
            val args = mutableListOf<String>()
            if (action.minProportion != null) {
                args.add("minProportion:=${action.minProportion}")
            }
            if (action.fields.isNotEmpty()) {
                val fields = action.fields.joinToString(", ")
                args.add("fields:={$fields}")
            }
            append(args.joinToString(", "))
            append(")")
        }

    private fun serializeAminoAcidMutationsAction(action: SiloAction.AminoAcidMutationsAction) =
        buildString {
            append(".aminoAcidMutations(")
            val args = mutableListOf<String>()
            if (action.minProportion != null) {
                args.add("minProportion:=${action.minProportion}")
            }
            if (action.fields.isNotEmpty()) {
                val fields = action.fields.joinToString(", ")
                args.add("fields:={$fields}")
            }
            append(args.joinToString(", "))
            append(")")
        }

    private fun serializeSequenceAction(action: SiloAction.SequenceAction): String {
        val allFields = action.additionalFields + action.sequenceNames
        val fields = allFields.joinToString(", ")
        return ".project({$fields})"
    }

    private fun serializeMRCAAction(action: SiloAction.MostRecentCommonAncestorAction) =
        buildString {
            val col = str(action.columnName)
            append(".mostRecentCommonAncestor($col")
            if (action.printNodesNotInTree == true) {
                append(", printNodesNotInTree:=true")
            }
            append(")")
        }

    private fun serializePhyloSubtreeAction(action: SiloAction.PhyloSubtreeAction) =
        buildString {
            val col = str(action.columnName)
            append(".phyloSubtree($col")
            if (action.printNodesNotInTree == true) {
                append(", printNodesNotInTree:=true")
            }
            append(")")
        }

    private fun serializePipelineSuffix(action: CommonActionFields) =
        buildString {
            if (action.orderByFields.isNotEmpty()) {
                val fields = action.orderByFields.joinToString(", ") {
                    serializeOrderByField(it)
                }
                append(".orderBy({$fields})")
            }
            if (action.offset != null) {
                append(".offset(${action.offset})")
            }
            if (action.limit != null) {
                append(".limit(${action.limit})")
            }
            when (val randomize = action.randomize) {
                is RandomizeConfig.Enabled -> append(".randomize()")
                is RandomizeConfig.WithSeed -> append(".randomize(seed:=${randomize.seed})")
                is RandomizeConfig.Disabled, null -> {}
            }
        }

    private fun serializeOrderByField(field: OrderByField): String =
        when (field.order) {
            Order.ASCENDING -> field.field
            Order.DESCENDING -> "${field.field}.desc()"
        }

    private fun str(value: String) = "'" + value.replace("'", "''") + "'"

    private fun dateLiteral(date: LocalDate?) = if (date == null) "null" else "'$date'::date"

    /**
     * Wraps sub-expressions in parentheses when needed to preserve precedence.
     * And/Or children that are themselves And/Or need wrapping to avoid ambiguity.
     */
    private fun wrapIfNeeded(expression: SiloFilterExpression): String {
        val serialized = serializeFilter(expression)!!
        return when (expression) {
            is Or, is And -> "($serialized)"
            else -> serialized
        }
    }
}
