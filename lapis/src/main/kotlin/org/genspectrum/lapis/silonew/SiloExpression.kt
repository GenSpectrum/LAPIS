package org.genspectrum.lapis.silonew

import java.time.LocalDate

internal const val OR_PRECEDENCE = 10
internal const val AND_PRECEDENCE = 20
internal const val NOT_PRECEDENCE = 30
internal const val COMPARISON_PRECEDENCE = 40
internal const val POSTFIX_PRECEDENCE = 50
internal const val ATOMIC_PRECEDENCE = 60

sealed class SiloExpression {
    internal abstract val precedence: Int

    internal abstract fun renderSelf(): String

    internal fun renderIn(parentPrecedence: Int): String =
        renderSelf().let { rendered ->
            if (precedence < parentPrecedence) "($rendered)" else rendered
        }

    fun render(): String = renderSelf()
}

open class SiloField internal constructor(
    open val name: String,
) : SiloExpression() {
    internal override val precedence = ATOMIC_PRECEDENCE

    internal override fun renderSelf() = renderQuotedIdentifier(name)

    override fun equals(other: Any?) = other is SiloField && other.javaClass == javaClass && name == other.name

    override fun hashCode() = name.hashCode()

    override fun toString() = "SiloField(name=$name)"
}

/** Creates an escaped reference to a SILO field. */
fun field(name: String): SiloField = SiloField(name)

private data object NullLiteral : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = "null"
}

private data class BooleanLiteral(
    val value: Boolean,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = value.toString()
}

private data class IntegerLiteral(
    val value: Long,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = value.toString()
}

private data class FloatingPointLiteral(
    val value: String,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = value
}

private data class StringLiteral(
    val value: String,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = "'${value.replace("'", "''")}'"
}

private data class DateLiteral(
    val value: LocalDate,
) : SiloExpression() {
    override val precedence = POSTFIX_PRECEDENCE

    override fun renderSelf() = "'$value'::date"
}

fun nullLiteral(): SiloExpression = NullLiteral

fun literal(value: Boolean): SiloExpression = BooleanLiteral(value)

fun literal(value: Int): SiloExpression = IntegerLiteral(value.toLong())

fun literal(value: Long): SiloExpression = IntegerLiteral(value)

fun literal(value: Float): SiloExpression {
    require(value.isFinite()) { "SaneQL floating-point literals must be finite" }
    return FloatingPointLiteral(value.toString())
}

fun literal(value: Double): SiloExpression {
    require(value.isFinite()) { "SaneQL floating-point literals must be finite" }
    return FloatingPointLiteral(value.toString())
}

fun literal(value: String): SiloExpression = StringLiteral(value)

fun literal(value: LocalDate): SiloExpression = DateLiteral(value)

class SiloSet internal constructor(
    val elements: List<SiloExpression>,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = elements.joinToString(prefix = "{", postfix = "}") { it.render() }

    override fun equals(other: Any?) = other is SiloSet && elements == other.elements

    override fun hashCode() = elements.hashCode()

    override fun toString() = "SiloSet(elements=$elements)"
}

fun set(vararg elements: SiloExpression): SiloSet = SiloSet(elements.toList())

fun set(elements: Iterable<SiloExpression>): SiloSet = SiloSet(elements.toList())

data class SiloAssignment(
    val field: SiloField,
    val value: SiloExpression,
) {
    internal fun render() = "${renderQuotedIdentifier(field.name)}:=${value.render()}"
}

infix fun SiloField.assign(value: SiloExpression): SiloAssignment = SiloAssignment(field = this, value = value)

fun assignment(
    name: String,
    value: SiloExpression,
): SiloAssignment = field(name) assign value

class SiloRecord internal constructor(
    val assignments: List<SiloAssignment>,
) : SiloExpression() {
    override val precedence = ATOMIC_PRECEDENCE

    override fun renderSelf() = assignments.joinToString(prefix = "{", postfix = "}") { it.render() }

    override fun equals(other: Any?) = other is SiloRecord && assignments == other.assignments

    override fun hashCode() = assignments.hashCode()

    override fun toString() = "SiloRecord(assignments=$assignments)"
}

fun record(vararg assignments: SiloAssignment): SiloRecord = SiloRecord(assignments.toList())

fun record(assignments: Iterable<SiloAssignment>): SiloRecord = SiloRecord(assignments.toList())

data class SiloNamedArgument(
    val name: String,
    val value: SiloExpression,
) {
    internal fun render() = "${renderIdentifier(name)}:=${value.render()}"
}

fun namedArgument(
    name: String,
    value: SiloExpression,
): SiloNamedArgument = SiloNamedArgument(name = name, value = value)

private enum class BinaryOperator(
    val token: String,
    val operatorPrecedence: Int,
) {
    EQUALS("=", COMPARISON_PRECEDENCE),
    NOT_EQUALS("<>", COMPARISON_PRECEDENCE),
    LESS_THAN("<", COMPARISON_PRECEDENCE),
    LESS_THAN_OR_EQUAL("<=", COMPARISON_PRECEDENCE),
    GREATER_THAN(">", COMPARISON_PRECEDENCE),
    GREATER_THAN_OR_EQUAL(">=", COMPARISON_PRECEDENCE),
    AND("&&", AND_PRECEDENCE),
    OR("||", OR_PRECEDENCE),
}

private data class BinaryExpression(
    val left: SiloExpression,
    val operator: BinaryOperator,
    val right: SiloExpression,
) : SiloExpression() {
    override val precedence = operator.operatorPrecedence

    override fun renderSelf() = "${left.renderIn(precedence)} ${operator.token} ${right.renderIn(precedence)}"
}

private data class NotExpression(
    val child: SiloExpression,
) : SiloExpression() {
    override val precedence = NOT_PRECEDENCE

    override fun renderSelf() = "!(${child.render()})"
}

infix fun SiloExpression.eq(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.EQUALS, right = other)

infix fun SiloExpression.neq(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.NOT_EQUALS, right = other)

infix fun SiloExpression.lt(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.LESS_THAN, right = other)

infix fun SiloExpression.lte(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.LESS_THAN_OR_EQUAL, right = other)

infix fun SiloExpression.gt(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.GREATER_THAN, right = other)

infix fun SiloExpression.gte(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.GREATER_THAN_OR_EQUAL, right = other)

infix fun SiloExpression.and(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.AND, right = other)

infix fun SiloExpression.or(other: SiloExpression): SiloExpression =
    BinaryExpression(left = this, operator = BinaryOperator.OR, right = other)

operator fun SiloExpression.not(): SiloExpression = NotExpression(this)

private data class FunctionCall(
    val name: String,
    val positionalArguments: List<SiloExpression>,
    val namedArguments: List<SiloNamedArgument>,
) : SiloExpression() {
    override val precedence = POSTFIX_PRECEDENCE

    override fun renderSelf() = renderCall(name, positionalArguments, namedArguments)
}

private data class MethodCall(
    val receiver: SiloExpression,
    val name: String,
    val positionalArguments: List<SiloExpression>,
    val namedArguments: List<SiloNamedArgument>,
) : SiloExpression() {
    override val precedence = POSTFIX_PRECEDENCE

    override fun renderSelf() = renderCall(receiver, name, positionalArguments, namedArguments)
}

/** Builds a scalar function call from a name and structured, escaped arguments. */
fun call(
    name: String,
    positionalArguments: List<SiloExpression> = emptyList(),
    namedArguments: List<SiloNamedArgument> = emptyList(),
): SiloExpression =
    functionCall(
        name = name,
        positionalArguments = positionalArguments,
        namedArguments = namedArguments,
    )

internal fun functionCall(
    name: String,
    positionalArguments: List<SiloExpression> = emptyList(),
    namedArguments: List<SiloNamedArgument> = emptyList(),
): SiloExpression =
    FunctionCall(
        name = name,
        positionalArguments = positionalArguments,
        namedArguments = namedArguments,
    )

/** Builds a scalar method call from a name and structured, escaped arguments. */
fun SiloExpression.call(
    name: String,
    positionalArguments: List<SiloExpression> = emptyList(),
    namedArguments: List<SiloNamedArgument> = emptyList(),
): SiloExpression =
    MethodCall(
        receiver = this,
        name = name,
        positionalArguments = positionalArguments,
        namedArguments = namedArguments,
    )

internal fun renderCall(
    name: String,
    positionalArguments: List<SiloExpression>,
    namedArguments: List<SiloNamedArgument>,
) = "${renderIdentifier(name)}(${renderArguments(positionalArguments, namedArguments)})"

internal fun renderCall(
    receiver: SiloExpression,
    name: String,
    positionalArguments: List<SiloExpression>,
    namedArguments: List<SiloNamedArgument>,
) = "${receiver.renderIn(POSTFIX_PRECEDENCE)}.${renderCall(name, positionalArguments, namedArguments)}"

private fun renderArguments(
    positionalArguments: List<SiloExpression>,
    namedArguments: List<SiloNamedArgument>,
) = (positionalArguments.map(SiloExpression::render) + namedArguments.map(SiloNamedArgument::render)).joinToString()

internal fun renderQuotedIdentifier(value: String) = "\"${value.replace("\"", "\"\"")}\""

internal fun renderIdentifier(value: String): String =
    if (value.matches(IDENTIFIER_PATTERN) && value !in RESERVED_IDENTIFIERS) {
        value
    } else {
        renderQuotedIdentifier(value)
    }

private val IDENTIFIER_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*")
private val RESERVED_IDENTIFIERS = setOf("true", "false", "null")
