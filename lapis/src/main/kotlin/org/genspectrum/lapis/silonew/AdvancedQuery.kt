package org.genspectrum.lapis.silonew

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.collections.ArrayDeque

class AdvancedQueryParseException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/**
 * Parses LAPIS advanced-query syntax into a structured, safely escaped SILO expression.
 *
 * [table] supplies metadata types and identifies nucleotide sequence names through `unaligned_<name>` columns.
 */
fun parseAdvancedQuery(
    advancedQuery: String,
    table: SiloTable,
): SiloExpression {
    if (advancedQuery.isBlank()) {
        throw AdvancedQueryParseException("Advanced query must not be blank.")
    }

    try {
        val errorListener = ThrowingAdvancedQueryErrorListener()
        val lexer = AdvancedQueryLexer(CharStreams.fromString(advancedQuery)).apply {
            removeErrorListeners()
            addErrorListener(errorListener)
        }
        val parser = AdvancedQueryParser(CommonTokenStream(lexer)).apply {
            removeErrorListeners()
            addErrorListener(errorListener)
        }
        val listener = SiloAdvancedQueryListener(table)
        ParseTreeWalker.DEFAULT.walk(listener, parser.start())
        return listener.result()
    } catch (exception: AdvancedQueryParseException) {
        throw exception
    } catch (exception: IllegalArgumentException) {
        throw AdvancedQueryParseException("Failed to parse advanced query: ${exception.message}", exception)
    }
}

private class ThrowingAdvancedQueryErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        message: String?,
        exception: RecognitionException?,
    ): Unit =
        throw AdvancedQueryParseException(
            "Failed to parse advanced query (line $line:$charPositionInLine): $message.",
            exception,
        )
}

private class SiloAdvancedQueryListener(
    private val table: SiloTable,
) : AdvancedQueryBaseListener() {
    private val expressions = ArrayDeque<SiloExpression>()
    private val columnsByName = table.columns.associateBy { it.name.lowercase(Locale.US) }

    fun result(): SiloExpression =
        expressions.singleOrNull()
            ?: throw AdvancedQueryParseException("Failed to parse advanced query into exactly one expression.")

    override fun enterMetadataQuery(ctx: AdvancedQueryParser.MetadataQueryContext) {
        val queriedName = ctx.name().text
        val value = decodeValue(ctx.value().text)

        when {
            queriedName.endsWith(REGEX_SUFFIX, ignoreCase = true) -> {
                val metadata = metadataField(queriedName.dropLast(REGEX_SUFFIX.length))
                expressions.addLast(field(metadata.name).like(value))
            }

            queriedName.endsWith(PHYLO_DESCENDANT_SUFFIX, ignoreCase = true) -> {
                val metadata = metadataField(queriedName.dropLast(PHYLO_DESCENDANT_SUFFIX.length))
                expressions.addLast(field(metadata.name).phyloDescendantOf(value))
            }

            else -> expressions.addLast(equality(metadataField(queriedName), value))
        }
    }

    override fun enterMetadataGreaterThanEqualQuery(ctx: AdvancedQueryParser.MetadataGreaterThanEqualQueryContext) {
        expressions.addLast(range(ctx.name()[0].text, ctx.name()[1].text, lowerBound = true))
    }

    override fun enterMetadataLessThanEqualQuery(ctx: AdvancedQueryParser.MetadataLessThanEqualQueryContext) {
        expressions.addLast(range(ctx.name()[0].text, ctx.name()[1].text, lowerBound = false))
    }

    override fun enterIsNullQuery(ctx: AdvancedQueryParser.IsNullQueryContext) {
        expressions.addLast(field(metadataField(ctx.name().text).name).isNull())
    }

    override fun enterSingleSegmentedMutationQuery(ctx: AdvancedQueryParser.SingleSegmentedMutationQueryContext) {
        val position = parsePosition(ctx.position().text)
        val symbol = ctx.singleSegmentedMutationQuerySecondSymbol()?.text?.uppercase(Locale.US)
        expressions.addLast(
            symbol?.let { nucleotideEquals(position = position, symbol = it) }
                ?: hasMutation(position = position),
        )
    }

    override fun enterNucleotideInsertionQuery(ctx: AdvancedQueryParser.NucleotideInsertionQueryContext) {
        expressions.addLast(
            insertionContains(
                position = parsePosition(ctx.position().text),
                value = ctx.nucleotideInsertionSymbol().joinToString(separator = "") { mapInsertionSymbol(it.text) },
            ),
        )
    }

    override fun enterNamedMutationQuery(ctx: AdvancedQueryParser.NamedMutationQueryContext) {
        val sequenceName = ctx.name().text
        val isNucleotide = isNucleotideSequence(sequenceName)
        val position = parsePosition(ctx.position().text)
        val symbol = ctx.mutationQuerySecondSymbol()?.text?.uppercase(Locale.US)

        expressions.addLast(
            when {
                isNucleotide -> {
                    symbol?.forEach(::validateNucleotideSymbol)
                    symbol?.let { nucleotideEquals(position = position, symbol = it, sequenceName = sequenceName) }
                        ?: hasMutation(position = position, sequenceName = sequenceName)
                }

                else -> {
                    symbol?.forEach(::validateAminoAcidSymbol)
                    symbol?.let { aminoAcidEquals(position = position, symbol = it, sequenceName = sequenceName) }
                        ?: hasAminoAcidMutation(position = position, sequenceName = sequenceName)
                }
            },
        )
    }

    override fun enterNamedInsertionQuery(ctx: AdvancedQueryParser.NamedInsertionQueryContext) {
        val sequenceName = ctx.name().text
        val isNucleotide = isNucleotideSequence(sequenceName)
        val rawValue = ctx.namedInsertionSymbol().joinToString(separator = "") { it.text.uppercase(Locale.US) }
        val value = ctx.namedInsertionSymbol().joinToString(separator = "") { mapInsertionSymbol(it.text) }
        val position = parsePosition(ctx.position().text)

        expressions.addLast(
            when {
                isNucleotide -> {
                    rawValue.forEach(::validateNucleotideSymbol)
                    insertionContains(position = position, value = value, sequenceName = sequenceName)
                }

                else -> {
                    rawValue.forEach(::validateAminoAcidSymbol)
                    aminoAcidInsertionContains(position = position, value = value, sequenceName = sequenceName)
                }
            },
        )
    }

    override fun exitAnd(ctx: AdvancedQueryParser.AndContext?) = combineWith(SiloExpression::and)

    override fun exitVariantAnd(ctx: AdvancedQueryParser.VariantAndContext?) = combineWith(SiloExpression::and)

    override fun exitOr(ctx: AdvancedQueryParser.OrContext?) = combineWith(SiloExpression::or)

    override fun exitVariantOr(ctx: AdvancedQueryParser.VariantOrContext?) = combineWith(SiloExpression::or)

    override fun exitNot(ctx: AdvancedQueryParser.NotContext?) = negateLast()

    override fun exitVariantNot(ctx: AdvancedQueryParser.VariantNotContext?) = negateLast()

    override fun exitMaybe(ctx: AdvancedQueryParser.MaybeContext?) = replaceLast(::maybe)

    override fun exitVariantMaybe(ctx: AdvancedQueryParser.VariantMaybeContext?) = replaceLast(::maybe)

    override fun exitNOfQuery(ctx: AdvancedQueryParser.NOfQueryContext) {
        val childCount = ctx.nOfExprs().expr().size
        val children = List(childCount) { expressions.removeLast() }.reversed()
        expressions.addLast(
            nOf(
                count = parsePosition(ctx.nOfNumberOfMatchers().text),
                children = children,
                matchExactly = ctx.nOfMatchExactly()?.let { true },
            ),
        )
    }

    private fun equality(
        metadata: AdvancedQueryMetadata,
        value: String,
    ): SiloExpression {
        val metadataField = field(metadata.name)
        val parsedValue = metadata.toLiteral(value)
        return if (metadata.type == SiloColumnType.DATE32) {
            metadataField.between(parsedValue, parsedValue)
        } else {
            metadataField eq parsedValue
        }
    }

    private fun range(
        queriedName: String,
        value: String,
        lowerBound: Boolean,
    ): SiloExpression {
        val metadata = metadataField(queriedName)
        val parsedValue = metadata.toLiteral(value)
        return if (lowerBound) {
            field(metadata.name).between(parsedValue, nullLiteral())
        } else {
            field(metadata.name).between(nullLiteral(), parsedValue)
        }
    }

    private fun metadataField(queriedName: String): AdvancedQueryMetadata =
        columnsByName[queriedName.lowercase(Locale.US)]
            ?.let { AdvancedQueryMetadata(name = it.name, type = it.type) }
            ?: AdvancedQueryMetadata(name = queriedName, type = SiloColumnType.STRING)

    private fun isNucleotideSequence(name: String): Boolean =
        columnsByName.containsKey("unaligned_$name".lowercase(Locale.US))

    private fun combineWith(operator: (SiloExpression, SiloExpression) -> SiloExpression) {
        val right = expressions.removeLast()
        val left = expressions.removeLast()
        expressions.addLast(operator(left, right))
    }

    private fun negateLast() = replaceLast { !it }

    private fun replaceLast(transform: (SiloExpression) -> SiloExpression) {
        expressions.addLast(transform(expressions.removeLast()))
    }
}

private data class AdvancedQueryMetadata(
    val name: String,
    val type: SiloColumnType,
) {
    fun toLiteral(value: String): SiloExpression =
        when (type) {
            SiloColumnType.STRING,
            SiloColumnType.INDEXED_STRING,
            -> literal(value)
            SiloColumnType.DATE32 -> literal(parseDate(value))
            SiloColumnType.BOOL -> literal(parseBoolean(value))
            SiloColumnType.INT32 -> literal(parseInt(value))
            SiloColumnType.INT64 -> literal(parseLong(value))
            SiloColumnType.FLOAT -> literal(parseDouble(value))
        }
}

private fun decodeValue(rawValue: String): String {
    if (!rawValue.startsWith('\'')) {
        return rawValue
    }
    return rawValue.substring(1, rawValue.length - 1).replace(ESCAPE_SEQUENCE_REGEX, "$1")
}

private fun parsePosition(value: String): Int =
    value.toIntOrNull()
        ?: throw AdvancedQueryParseException("'$value' is not a valid position.")

private fun parseBoolean(value: String): Boolean =
    try {
        value.lowercase(Locale.US).toBooleanStrict()
    } catch (exception: IllegalArgumentException) {
        throw AdvancedQueryParseException("'$value' is not a valid boolean.", exception)
    }

private fun parseDate(value: String): LocalDate =
    try {
        LocalDate.parse(value)
    } catch (exception: DateTimeParseException) {
        throw AdvancedQueryParseException("'$value' is not a valid date.", exception)
    }

private fun parseInt(value: String): Int =
    value.toIntOrNull()
        ?: throw AdvancedQueryParseException("'$value' is not a valid integer.")

private fun parseLong(value: String): Long =
    value.toLongOrNull()
        ?: throw AdvancedQueryParseException("'$value' is not a valid long integer.")

private fun parseDouble(value: String): Double {
    val parsed = value.toDoubleOrNull()
        ?: throw AdvancedQueryParseException("'$value' is not a valid floating-point number.")
    if (!parsed.isFinite()) {
        throw AdvancedQueryParseException("'$value' is not a finite floating-point number.")
    }
    return parsed
}

private fun mapInsertionSymbol(symbol: String): String =
    when (symbol) {
        "*" -> "\\*"
        "?" -> ".*"
        else -> symbol
    }.uppercase(Locale.US)

private fun validateNucleotideSymbol(symbol: Char) {
    if (symbol.uppercaseChar() !in NUCLEOTIDE_SYMBOLS) {
        throw AdvancedQueryParseException("Invalid nucleotide symbol: $symbol.")
    }
}

private fun validateAminoAcidSymbol(symbol: Char) {
    if (symbol.uppercaseChar() !in AMINO_ACID_SYMBOLS) {
        throw AdvancedQueryParseException("Invalid amino-acid symbol: $symbol.")
    }
}

private const val REGEX_SUFFIX = ".regex"
private const val PHYLO_DESCENDANT_SUFFIX = ".PhyloDescendantOf"
private val ESCAPE_SEQUENCE_REGEX = Regex("""\\(.)""")
private val NUCLEOTIDE_SYMBOLS = "ACGTMRWSYKVHDBN-.?".toSet()
private val AMINO_ACID_SYMBOLS = "ARNDCEQGHILKMFPSTWYVBZX-.?*".toSet()
