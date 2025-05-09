package org.genspectrum.lapis.model

import AdvancedQueryBaseListener
import AdvancedQueryParser.AndContext
import AdvancedQueryParser.MaybeContext
import AdvancedQueryParser.NOfQueryContext
import AdvancedQueryParser.NamedInsertionQueryContext
import AdvancedQueryParser.NotContext
import AdvancedQueryParser.NucleotideInsertionQueryContext
import AdvancedQueryParser.NucleotideMutationQueryContext
import AdvancedQueryParser.OrContext
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.DatabaseMetadata
import org.genspectrum.lapis.config.MetadataType
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.log
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
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.StringSearch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.collections.ArrayDeque

val nucleotideSymbols = setOf('A', 'C', 'G', 'T')
val ambiguousNucSymbols = setOf('M', 'R', 'W', 'S', 'Y', 'K', 'V', 'H', 'D', 'B', 'N', '-', '.', '?')
val aaSymbols = setOf(
    'A',
    'R',
    'N',
    'D',
    'C',
    'E',
    'Q',
    'G',
    'H',
    'I',
    'L',
    'K',
    'M',
    'F',
    'P',
    'S',
    'T',
    'W',
    'Y',
    'V',
    '*',
)
val ambiguousAaSymbols = setOf('X', '-', '.', '?')

fun validateNucleotideSymbol(c: Char) {
    if (c.uppercaseChar() !in ambiguousNucSymbols && c.uppercaseChar() !in nucleotideSymbols) {
        throw BadRequestException("Invalid nucleotide symbol: $c")
    }
}

fun validateAminoAcidSymbol(c: Char) {
    if (c.uppercaseChar() !in ambiguousAaSymbols && c.uppercaseChar() !in aaSymbols) {
        throw BadRequestException("Invalid amino acid symbol: $c")
    }
}

class AdvancedQueryCustomListener(
    val referenceGenomeSchema: ReferenceGenomeSchema,
    databaseConfig: DatabaseConfig,
) : AdvancedQueryBaseListener(),
    ParseTreeListener {
    private val expressionStack = ArrayDeque<SiloFilterExpression>()

    private val metadataFieldsByName = databaseConfig.schema.metadata
        .associateBy { it.name.lowercase(Locale.US) }

    fun getAdvancedQueryExpression(): SiloFilterExpression {
        if (expressionStack.size != 1) {
            log.error { "Expected exactly one expression on the stack, but got this stack $expressionStack." }
            throw RuntimeException("Failed to parse advanced query.")
        }

        return expressionStack.first()
    }

    override fun enterMetadataLessThanEqualQuery(ctx: AdvancedQueryParser.MetadataLessThanEqualQueryContext) {
        val metadataName = ctx.name()[0].text
        val metadataValue = ctx.name()[1].text

        val field: DatabaseMetadata =
            metadataFieldsByName[metadataName.lowercase(Locale.US)]
                ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            MetadataType.DATE -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, from = null, to = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            MetadataType.FLOAT -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, from = null, to = metadataValue.toDouble()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            MetadataType.INT -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, to = metadataValue.toInt(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            else -> {
                throw BadRequestException("expression <= cannot be used for ${field.type}", null)
            }
        }
    }

    override fun enterMetadataGreaterThanEqualQuery(ctx: AdvancedQueryParser.MetadataGreaterThanEqualQueryContext) {
        val metadataName = ctx.name()[0].text
        val metadataValue = ctx.name()[1].text

        val field: DatabaseMetadata =
            metadataFieldsByName[metadataName.lowercase(Locale.US)]
                ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            MetadataType.DATE -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = null, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            MetadataType.FLOAT -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, from = metadataValue.toDouble(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            MetadataType.INT -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, from = metadataValue.toInt(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            else -> {
                throw BadRequestException("expression >= cannot be used for ${field.type}", null)
            }
        }
    }

    private fun mapToLineageFilter(
        metadataName: String,
        metadataValue: String?,
    ): LineageEquals {
        val sanitizedValue = metadataValue?.removeSurrounding("'")

        return when {
            sanitizedValue.isNullOrBlank() -> throw BadRequestException(
                "Invalid lineage: $sanitizedValue is NULL - to search for NULL values use `IsNull($metadataName)`?",
            )
            sanitizedValue.endsWith(
                ".*",
            ) -> LineageEquals(metadataName, sanitizedValue.substringBeforeLast(".*"), includeSublineages = true)
            sanitizedValue.endsWith("*") -> LineageEquals(
                metadataName,
                sanitizedValue.substringBeforeLast("*"),
                includeSublineages = true,
            )
            sanitizedValue.endsWith('.') -> throw BadRequestException(
                "Invalid lineage: $sanitizedValue must not end with a dot. Did you mean '$sanitizedValue*'?",
            )
            else -> LineageEquals(metadataName, sanitizedValue, includeSublineages = false)
        }
    }

    override fun enterMetadataQuery(ctx: AdvancedQueryParser.MetadataQueryContext) {
        val metadataName = ctx.name().text
        val metadataValue = ctx.value().text.trim('\'')

        var name = metadataName
        if (metadataName.endsWith(".regex")) {
            name = metadataName.substringBeforeLast(".regex")
        }

        val field: DatabaseMetadata =
            metadataFieldsByName[name.lowercase(Locale.US)]
                ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            MetadataType.STRING -> {
                if (field.generateLineageIndex) {
                    expressionStack.addLast(mapToLineageFilter(field.name, metadataValue))
                } else {
                    if (field.lapisAllowsRegexSearch and metadataName.endsWith(".regex")) {
                        expressionStack.addLast(StringSearch(field.name, metadataValue))
                    } else if (metadataName.endsWith(".regex")) {
                        throw BadRequestException(
                            "Metadata field `${field.name}` does not support regex search.",
                            null,
                        )
                    } else {
                        expressionStack.addLast(StringEquals(field.name, metadataValue))
                    }
                }
            }

            MetadataType.BOOLEAN -> {
                try {
                    expressionStack.addLast(BooleanEquals(field.name, metadataValue.toBoolean()))
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("'$metadataValue' is not a valid boolean", e)
                }
            }

            MetadataType.DATE -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = date, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            MetadataType.FLOAT -> {
                try {
                    expressionStack.addLast(FloatEquals(field.name, metadataValue.toDouble()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            MetadataType.INT -> {
                try {
                    expressionStack.addLast(IntEquals(field.name, metadataValue.toInt()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }
        }
    }

    override fun enterIsNullQuery(ctx: AdvancedQueryParser.IsNullQueryContext) {
        val metadataName = ctx.name().text

        var name = metadataName
        if (metadataName.endsWith(".regex")) {
            name = metadataName.substringBeforeLast(".regex")
        }

        val field: DatabaseMetadata =
            metadataFieldsByName[name.lowercase(Locale.US)]
                ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            MetadataType.STRING -> {
                if (field.generateLineageIndex) {
                    expressionStack.addLast(LineageEquals(field.name, null, false))
                } else {
                    if (metadataName.endsWith(".regex")) {
                        throw BadRequestException("Filter IsNull($name) instead of IsNull($metadataName)", null)
                    }
                    expressionStack.addLast(StringEquals(field.name, null))
                }
            }

            MetadataType.BOOLEAN -> {
                expressionStack.addLast(BooleanEquals(field.name, null))
            }

            MetadataType.DATE -> {
                expressionStack.addLast(Not(DateBetween(field.name, to = null, from = null)))
            }

            MetadataType.FLOAT -> {
                expressionStack.addLast(FloatEquals(field.name, null))
            }

            MetadataType.INT -> {
                expressionStack.addLast(IntEquals(field.name, null))
            }
        }
    }

    override fun enterNucleotideMutationQuery(ctx: NucleotideMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        val position = ctx.position().text.toInt()

        val expression = when (val secondSymbol = ctx.nucleotideMutationQuerySecondSymbol()) {
            null -> HasNucleotideMutation(null, position)
            else -> NucleotideSymbolEquals(null, position, secondSymbol.text.uppercase())
        }

        expressionStack.addLast(expression)
    }

    override fun exitAnd(ctx: AndContext?) {
        val lastChildren = when (val last = expressionStack.removeLast()) {
            is And -> last.children
            else -> listOf(last)
        }

        val secondLastChildren = when (val secondLast = expressionStack.removeLast()) {
            is And -> secondLast.children
            else -> listOf(secondLast)
        }

        expressionStack.addLast(And(lastChildren + secondLastChildren))
    }

    override fun exitVariantAnd(ctx: AdvancedQueryParser.VariantAndContext?) {
        val lastChildren = when (val last = expressionStack.removeLast()) {
            is And -> last.children
            else -> listOf(last)
        }

        val secondLastChildren = when (val secondLast = expressionStack.removeLast()) {
            is And -> secondLast.children
            else -> listOf(secondLast)
        }

        expressionStack.addLast(And(lastChildren + secondLastChildren))
    }

    override fun exitNot(ctx: NotContext?) {
        val child = expressionStack.removeLast()
        expressionStack.addLast(Not(child))
    }

    override fun exitVariantNot(ctx: AdvancedQueryParser.VariantNotContext?) {
        val child = expressionStack.removeLast()
        expressionStack.addLast(Not(child))
    }

    override fun exitOr(ctx: OrContext?) {
        val lastChildren = when (val last = expressionStack.removeLast()) {
            is Or -> last.children
            else -> listOf(last)
        }

        val secondLastChildren = when (val secondLast = expressionStack.removeLast()) {
            is Or -> secondLast.children
            else -> listOf(secondLast)
        }

        expressionStack.addLast(Or(lastChildren + secondLastChildren))
    }

    override fun exitVariantOr(ctx: AdvancedQueryParser.VariantOrContext?) {
        val lastChildren = when (val last = expressionStack.removeLast()) {
            is Or -> last.children
            else -> listOf(last)
        }

        val secondLastChildren = when (val secondLast = expressionStack.removeLast()) {
            is Or -> secondLast.children
            else -> listOf(secondLast)
        }

        expressionStack.addLast(Or(lastChildren + secondLastChildren))
    }

    override fun exitMaybe(ctx: MaybeContext?) {
        val child = expressionStack.removeLast()
        expressionStack.addLast(Maybe(child))
    }

    override fun exitVariantMaybe(ctx: AdvancedQueryParser.VariantMaybeContext?) {
        val child = expressionStack.removeLast()
        expressionStack.addLast(Maybe(child))
    }

    override fun exitNOfQuery(ctx: NOfQueryContext?) {
        if (ctx == null) {
            return
        }

        val n = ctx.nOfNumberOfMatchers().text.toInt()
        val matchExactly = ctx.nOfMatchExactly() != null
        val nOfExprs = ctx.nOfExprs().expr().size

        val children = mutableListOf<SiloFilterExpression>()
        for (i in 1..nOfExprs) {
            children += expressionStack.removeLast()
        }

        expressionStack.addLast(NOf(n, matchExactly, children.reversed()))
    }

    override fun enterNucleotideInsertionQuery(ctx: NucleotideInsertionQueryContext) {
        val value = ctx.nucleotideInsertionSymbol().joinToString("", transform = ::mapInsertionSymbol)
        expressionStack.addLast(
            NucleotideInsertionContains(
                ctx.position().text.toInt(),
                value.uppercase(),
                null,
            ),
        )
    }

    override fun enterNamedMutationQuery(ctx: AdvancedQueryParser.NamedMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        val mutatedTo = ctx.mutationQuerySecondSymbol()?.text
        val mutatedFrom = ctx.mutationQueryFirstSymbol()?.text
        val position = ctx.position().text.toInt()
        val name = ctx.name().text

        // Ensure that the geneName is a valid gene or segment
        var gene: String? = null
        var segmentName: String? = null
        when {
            referenceGenomeSchema.hasGene(name) -> {
                gene = referenceGenomeSchema.getGene(name).name
            }
            referenceGenomeSchema.hasNucleotideSequence(name) -> {
                segmentName = referenceGenomeSchema.getNucleotideSequence(name).name
            }
            else -> {
                throw BadRequestException("$name is not a known segment or gene", null)
            }
        }

        if (gene != null) {
            // As the set of ambiguous aa and nuc mutations is disjoint, we need to check if the mutation is valid
            mutatedTo?.first()?.let { validateAminoAcidSymbol(it) }
            val expression = when (val aaSymbol = ctx.mutationQuerySecondSymbol()) {
                null -> HasAminoAcidMutation(gene, position)
                else -> AminoAcidSymbolEquals(gene, position, aaSymbol.text.uppercase())
            }
            expressionStack.addLast(expression)
        }
        if (segmentName != null) {
            // As nucleotide mutations are a subset of amino acid mutations, we need to check if the mutation is valid
            mutatedTo?.first()?.let { validateNucleotideSymbol(it) }
            val expression = when (val nucSymbol = ctx.mutationQuerySecondSymbol()) {
                null -> HasNucleotideMutation(segmentName, position)
                else -> NucleotideSymbolEquals(segmentName, position, nucSymbol.text.uppercase())
            }
            expressionStack.addLast(expression)
        }
    }

    override fun enterNamedInsertionQuery(ctx: NamedInsertionQueryContext) {
        val value = ctx.namedInsertionSymbol().joinToString("", transform = ::mapInsertionSymbol)
        val plainString = ctx.namedInsertionSymbol().joinToString(separator = "") { it.text.uppercase() }
        val name = ctx.name().text
        var gene: String? = null
        var sequenceName: String? = null
        try {
            gene = referenceGenomeSchema.getGene(name).name
        } catch (e: BadRequestException) {
            try {
                sequenceName = referenceGenomeSchema.getNucleotideSequence(name).name
            } catch (e: BadRequestException) {
                throw BadRequestException("$name is not a known segment or gene", null)
            }
        }
        if (gene != null) {
            plainString.forEach { validateAminoAcidSymbol(it) }
            expressionStack.addLast(
                AminoAcidInsertionContains(
                    ctx.position().text.toInt(),
                    value.uppercase(),
                    gene,
                ),
            )
        }
        if (sequenceName != null) {
            val sequenceName = referenceGenomeSchema.getNucleotideSequence(name).name
            plainString.forEach { validateNucleotideSymbol(it) }
            expressionStack.addLast(
                NucleotideInsertionContains(
                    ctx.position().text.toInt(),
                    value.uppercase(),
                    sequenceName,
                ),
            )
        }
    }
}
