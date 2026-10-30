package org.genspectrum.lapis.rhydb

import org.genspectrum.lapis.request.LineageWithSublineagesField
import org.genspectrum.lapis.response.COUNT_PROPERTY

/**
 * Appends the aggregation pipeline steps for this [RhyDbAction.AggregatedAction] to [builder]: a plain `group`, or,
 * when sublineage-inclusive lineage fields are requested, the closure-join pipeline from [sublineageAggregationSteps].
 */
internal fun RhyDbAction.AggregatedAction.applyAggregatedSteps(builder: PipelineBuilder): PipelineBuilder =
    if (lineageFields.isEmpty()) {
        plainAggregationSteps(builder)
    } else {
        sublineageAggregationSteps(builder)
    }

private fun RhyDbAction.AggregatedAction.plainAggregationSteps(builder: PipelineBuilder) =
    builder
        .applyComputedMappings(this)
        .group(by = plainGroupByColumns(), aggs = listOf(COUNT_PROPERTY to count()))

/**
 * For sublineage-inclusive aggregation we first count by the raw lineage value(s), then for each lineage field join
 * the (reflexive) transitive closure of its lineage tree and re-aggregate the counts under each ancestor. See
 * [sublineageClosureRelation] for the per-field closure. A left join keeps the `null`/unknown lineage group, matching
 * plain aggregation.
 */
private fun RhyDbAction.AggregatedAction.sublineageAggregationSteps(builder: PipelineBuilder): PipelineBuilder {
    var result = builder.applyComputedMappings(this)
    val lineageSourceColumns = lineageFields.map { id(it.fieldName) }
    result = result.group(by = plainGroupByColumns() + lineageSourceColumns, aggs = listOf("c" to count()))
    lineageFields.forEach { field ->
        result = result.join(
            relation = sublineageClosureRelation(field),
            on = id(joinOnColumnName(field)) eq id(field.fieldName),
            type = JoinType.LEFT,
        )
    }
    val groupByColumns = plainGroupByColumns() + lineageFields.map { id(it.outputColumnName) }
    return result.group(by = groupByColumns, aggs = listOf(COUNT_PROPERTY to sum("c")))
}

private fun PipelineBuilder.applyComputedMappings(action: RhyDbAction.AggregatedAction): PipelineBuilder {
    val allComputedMappings =
        action.sequencePositionFields.map { field ->
            field.outputColumnName to
                SaneQlMethodCall(id(field.sequenceName), "at", listOf(SaneQlInt(field.position)))
        } + action.computedFields.map { field ->
            field.outputColumnName to SaneQlMethodCall(id(field.sourceField), field.function.saneQlMethodName)
        }

    return if (allComputedMappings.isEmpty()) {
        this
    } else {
        map(allComputedMappings)
    }
}

private fun RhyDbAction.AggregatedAction.plainGroupByColumns() =
    groupByFields.map { id(it) } +
        sequencePositionFields.map { id(it.outputColumnName) } +
        computedFields.map { id(it.outputColumnName) }

/** The join key column into which a lineage field's closure maps its matched (alias) lineage values. */
private fun joinOnColumnName(field: LineageWithSublineagesField) = "_to_${field.fieldName}"

/**
 * The reflexive transitive closure of a lineage field's tree, one row per (ancestor, descendant) pair, plus extra
 * rows mapping each descendant's aliases, so that data stored under an alias still matches. Recombinant edges are
 * excluded (matching the `*` filter's `doNotFollow` default). The ancestor is exposed under the field's output column
 * name (e.g. `pangoLineage*`), with a trailing `*` appended via `concat` so values read as e.g. `B.1.1*`, and the
 * matched value under [joinOnColumnName]. See lineage_definitions.md for the relation/alias table schema.
 */
private fun sublineageClosureRelation(field: LineageWithSublineagesField): SaneQlExpression {
    val outputColumn = field.outputColumnName
    val joinColumn = joinOnColumnName(field)

    val transitiveClosure = pipeline(id(field.fieldName))
        .filter(SaneQlNot(id("is_recombinant_edge")))
        .transitiveClosure(parent = "parent", child = "lineage", includeVertices = true)

    val canonicalEdges = transitiveClosure
        .map(listOf(outputColumn to starSuffixed(id("from")), joinColumn to id("to")))
        .project(listOf(id(outputColumn), id(joinColumn)))

    val aliasEdges = transitiveClosure
        .join(relation = id("${field.fieldName}_aliases"), on = id("to") eq id("lineage"))
        .map(listOf(outputColumn to starSuffixed(id("from")), joinColumn to id("alias")))
        .project(listOf(id(outputColumn), id(joinColumn)))

    return unionAll(canonicalEdges, aliasEdges)
}

/** Wraps a string expression in `concat(expr, '*')` so each ancestor value reads as e.g. `B.1.1*`. */
private fun starSuffixed(expression: SaneQlExpression) = concat(expression, "*")
