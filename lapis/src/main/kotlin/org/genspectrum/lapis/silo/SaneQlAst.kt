package org.genspectrum.lapis.silo

import java.time.LocalDate

/** Marker for anything that renders to SaneQL text. */
sealed interface SaneQlNode {
    fun render(): String
}

/** A named argument in a function/method/step call, e.g. `minProportion:=0.5`. */
data class SaneQlNamedArg(
    val name: String,
    val value: SaneQlExpression,
)

sealed interface SaneQlExpression : SaneQlNode

data object SaneQlNull : SaneQlExpression {
    override fun render() = "null"
}

data class SaneQlBoolean(
    val value: Boolean,
) : SaneQlExpression {
    override fun render() = value.toString()
}

data class SaneQlInt(
    val value: Int,
) : SaneQlExpression {
    override fun render() = value.toString()
}

data class SaneQlFloat(
    val value: Double,
) : SaneQlExpression {
    override fun render() = value.toString()
}

/** A single-quoted string literal; embedded single quotes are escaped by doubling. */
data class SaneQlString(
    val value: String,
) : SaneQlExpression {
    override fun render() = "'" + value.replace("'", "''") + "'"
}

/** A `'yyyy-MM-dd'::date` literal, or `null` when [value] is null. */
data class SaneQlDate(
    val value: LocalDate?,
) : SaneQlExpression {
    override fun render() = if (value == null) "null" else "'$value'::date"
}

/** A double-quoted column identifier; embedded double quotes are escaped by doubling. Prevents SaneQL injection. */
data class SaneQlIdentifier(
    val name: String,
) : SaneQlExpression {
    override fun render() = "\"" + name.replace("\"", "\"\"") + "\""
}

/** A `{item1, item2, ...}` list/record literal. */
data class SaneQlList(
    val items: List<SaneQlExpression>,
) : SaneQlExpression {
    override fun render() = "{" + items.joinToString(", ") { it.render() } + "}"
}

/** A `name:=value` assignment, used inside [SaneQlList]s that act as records, e.g. `{count:=count()}`. */
data class SaneQlAssignment(
    val name: String,
    val value: SaneQlExpression,
) : SaneQlExpression {
    override fun render() = "$name:=${value.render()}"
}

/** `"column" = value` */
data class SaneQlEquals(
    val column: SaneQlExpression,
    val value: SaneQlExpression,
) : SaneQlExpression {
    override fun render() = "${column.render()} = ${value.render()}"
}

/** `!(child)` */
data class SaneQlNot(
    val child: SaneQlExpression,
) : SaneQlExpression {
    override fun render() = "!(${child.render()})"
}

/**
 * Variadic `&&` conjunction.
 */
data class SaneQlAnd(
    val children: List<SaneQlExpression>,
) : SaneQlExpression {
    override fun render() = children.joinToString(" && ") { wrapIfNeeded(it) }
}

/**
 * Variadic `||` disjunction.
 */
data class SaneQlOr(
    val children: List<SaneQlExpression>,
) : SaneQlExpression {
    override fun render() = children.joinToString(" || ") { wrapIfNeeded(it) }
}

private fun wrapIfNeeded(expression: SaneQlExpression): String {
    val rendered = expression.render()
    return when (expression) {
        is SaneQlAnd, is SaneQlOr -> "($rendered)"
        else -> rendered
    }
}

/** `name(args)`, e.g. `nucleotideEquals(position:=1234, symbol:='A')`. */
data class SaneQlFunctionCall(
    val name: String,
    val positionalArgs: List<SaneQlExpression> = emptyList(),
    val namedArgs: List<SaneQlNamedArg> = emptyList(),
) : SaneQlExpression {
    override fun render() = "$name(${renderArgs(positionalArgs, namedArgs)})"
}

/** `receiver.name(args)`, e.g. `"fieldName".between('2021-03-31'::date, null)`. */
data class SaneQlMethodCall(
    val receiver: SaneQlExpression,
    val name: String,
    val positionalArgs: List<SaneQlExpression> = emptyList(),
    val namedArgs: List<SaneQlNamedArg> = emptyList(),
) : SaneQlExpression {
    override fun render() = "${receiver.render()}.$name(${renderArgs(positionalArgs, namedArgs)})"
}

private fun renderArgs(
    positionalArgs: List<SaneQlExpression>,
    namedArgs: List<SaneQlNamedArg>,
): String =
    (positionalArgs.map { it.render() } + namedArgs.map { "${it.name}:=${it.value.render()}" })
        .joinToString(", ")

/**
 * One pipeline step chained after `default.filter(...)`, e.g. `.groupBy({count:=count()})`.
 * Callers that need no step at all (e.g. `DetailsAction` without fields) simply omit it from
 * [SaneQlPipeline.steps] - there is no "empty step" representation.
 */
data class SaneQlStep(
    val name: String,
    val positionalArgs: List<SaneQlExpression> = emptyList(),
    val namedArgs: List<SaneQlNamedArg> = emptyList(),
) : SaneQlNode {
    override fun render() = ".$name(${renderArgs(positionalArgs, namedArgs)})"
}

/** The full `default.filter(filter).step1.step2...` pipeline. */
data class SaneQlPipeline(
    val filter: SaneQlExpression,
    val steps: List<SaneQlStep>,
) : SaneQlNode {
    override fun render() = "default.filter(${filter.render()})" + steps.joinToString("") { it.render() }
}
