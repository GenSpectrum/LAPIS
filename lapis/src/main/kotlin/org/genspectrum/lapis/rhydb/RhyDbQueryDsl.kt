package org.genspectrum.lapis.rhydb

/*
 * A small DSL layer on top of SaneQlAst. It lets RhyDbQuery describe a SaneQL query with fluent Kotlin calls
 * (pipeline(id("data")).filter(...).group(...)) instead of hand-constructing SaneQlStep/SaneQlExpression trees.
 * The DSL only renders SaneQL; it carries no query semantics of its own.
 */

// Expression builders

/** A double-quoted column/table identifier, e.g. `"theColumn"`. */
fun id(name: String) = SaneQlIdentifier(name)

/** A single-quoted string literal, e.g. `'theValue'`. */
fun str(value: String) = SaneQlString(value)

/** An integer literal, or `null` when [value] is null. */
fun intOrNull(value: Int?): SaneQlExpression = if (value == null) SaneQlNull else SaneQlInt(value)

/** A float literal, or `null` when [value] is null. */
fun floatOrNull(value: Double?): SaneQlExpression = if (value == null) SaneQlNull else SaneQlFloat(value)

/** `isNull("column")`. */
fun isNull(column: String) = SaneQlFunctionCall("isNull", positionalArgs = listOf(id(column)))

/** `concat(expr, suffix)`, e.g. `concat("from", '*')`. */
fun concat(
    expression: SaneQlExpression,
    suffix: String,
) = SaneQlFunctionCall("concat", positionalArgs = listOf(expression, str(suffix)))

/** `unionall(a, b, ...)`. */
fun unionAll(vararg relations: SaneQlExpression) = SaneQlFunctionCall("unionall", positionalArgs = relations.toList())

/** `count()`. */
fun count() = SaneQlFunctionCall("count")

/** `sum("column")`. */
fun sum(column: String) = SaneQlFunctionCall("sum", positionalArgs = listOf(id(column)))

/** The join type keyword rendered as `type:=left`. */
enum class JoinType(
    val keyword: String,
) {
    LEFT("left"),
}

// Pipeline builder

/** Starts a pipeline from [receiver], e.g. `pipeline(id("data")).filter(...)...`. */
fun pipeline(receiver: SaneQlExpression) = PipelineBuilder(receiver, emptyList())

fun pipeline(table: String) = pipeline(id(table))

/**
 * A fluent builder for a [SaneQlPipeline]. Each method appends one SaneQL step and returns a new builder, so a
 * pipeline reads like the SaneQL it renders to. [build] (or [render]) produces the underlying [SaneQlPipeline].
 */
data class PipelineBuilder(
    private val receiver: SaneQlExpression,
    private val steps: List<SaneQlStep>,
) : SaneQlExpression {
    private fun add(step: SaneQlStep) = copy(steps = steps + step)

    /** Escape hatch for one-off steps that have no dedicated method, e.g. `.mutations(...)` or `.phyloSubtree(...)`. */
    fun step(
        name: String,
        positionalArgs: List<SaneQlExpression> = emptyList(),
        namedArgs: List<SaneQlNamedArg> = emptyList(),
    ) = add(SaneQlStep(name, positionalArgs = positionalArgs, namedArgs = namedArgs))

    fun filter(predicate: SaneQlExpression) = add(SaneQlStep("filter", positionalArgs = listOf(predicate)))

    /** `.group(by:={...}, aggs:={"name":=value, ...})`. */
    fun group(
        by: List<SaneQlExpression>,
        aggs: List<Pair<String, SaneQlExpression>>,
    ) = add(
        SaneQlStep(
            "group",
            namedArgs = listOf(
                SaneQlNamedArg("by", SaneQlList(by)),
                SaneQlNamedArg("aggs", record(aggs)),
            ),
        ),
    )

    /** `.map({"name":=value, ...})`. */
    fun map(assignments: List<Pair<String, SaneQlExpression>>) =
        add(SaneQlStep("map", positionalArgs = listOf(record(assignments))))

    /** `.project({"col1", "col2", ...})`. */
    fun project(columns: List<SaneQlExpression>) =
        add(SaneQlStep("project", positionalArgs = listOf(SaneQlList(columns))))

    /** `.join(relation, on, type:=...)`. */
    fun join(
        relation: SaneQlExpression,
        on: SaneQlExpression,
        type: JoinType? = null,
    ) = add(
        SaneQlStep(
            "join",
            positionalArgs = listOf(relation, on),
            namedArgs = type?.let { listOf(SaneQlNamedArg("type", SaneQlKeyword(it.keyword))) } ?: emptyList(),
        ),
    )

    /** `.transitiveClosure(parent, child, includeVertices:=...)`. */
    fun transitiveClosure(
        parent: String,
        child: String,
        includeVertices: Boolean,
    ) = add(
        SaneQlStep(
            "transitiveClosure",
            positionalArgs = listOf(SaneQlKeyword(parent), SaneQlKeyword(child)),
            namedArgs = listOf(SaneQlNamedArg("includeVertices", SaneQlBoolean(includeVertices))),
        ),
    )

    /** `.order(by:={...})`. */
    fun order(by: List<SaneQlExpression>) =
        add(SaneQlStep("order", namedArgs = listOf(SaneQlNamedArg("by", SaneQlList(by)))))

    fun offset(value: Int) = add(SaneQlStep("offset", positionalArgs = listOf(SaneQlInt(value))))

    fun limit(value: Int) = add(SaneQlStep("limit", positionalArgs = listOf(SaneQlInt(value))))

    /** `.randomize()` or `.randomize(seed:=...)` when [seed] is given. */
    fun randomize(seed: Int? = null) =
        add(
            SaneQlStep(
                "randomize",
                namedArgs = seed?.let { listOf(SaneQlNamedArg("seed", SaneQlInt(it))) } ?: emptyList(),
            ),
        )

    fun build() = SaneQlPipeline(receiver, steps)

    override fun render() = build().render()
}

/** A `{"name":=value, ...}` record literal from name/value pairs. */
private fun record(assignments: List<Pair<String, SaneQlExpression>>) =
    SaneQlList(assignments.map { SaneQlAssignment(it.first, it.second) })
