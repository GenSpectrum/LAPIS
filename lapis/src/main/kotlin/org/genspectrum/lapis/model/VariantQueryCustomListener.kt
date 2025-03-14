package org.genspectrum.lapis.model

import VariantQueryBaseListener
import VariantQueryParser.AaInsertionQueryContext
import VariantQueryParser.AaMutationQueryContext
import VariantQueryParser.AndContext
import VariantQueryParser.GisaidCladeNomenclatureContext
import VariantQueryParser.MaybeContext
import VariantQueryParser.NOfQueryContext
import VariantQueryParser.NextcladePangolineageQueryContext
import VariantQueryParser.NextstrainCladeQueryContext
import VariantQueryParser.NotContext
import VariantQueryParser.NucleotideInsertionQueryContext
import VariantQueryParser.NucleotideMutationQueryContext
import VariantQueryParser.OrContext
import VariantQueryParser.PangolineageQueryContext
import VariantQueryParser.PangolineageWithPossibleSublineagesContext
import mu.KotlinLogging
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.SequenceFilterField
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.request.ESCAPED_STOP_CODON
import org.genspectrum.lapis.request.LAPIS_INSERTION_AMBIGUITY_SYMBOL
import org.genspectrum.lapis.request.SILO_INSERTION_AMBIGUITY_SYMBOL
import org.genspectrum.lapis.request.STOP_CODON
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
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.collections.ArrayDeque

private val log = KotlinLogging.logger { }

class VariantQueryCustomListener(
    val referenceGenomeSchema: ReferenceGenomeSchema,
    private val allowedSequenceFilterFields: SequenceFilterFields,
) : VariantQueryBaseListener(),
    ParseTreeListener {
    private val expressionStack = ArrayDeque<SiloFilterExpression>()

    fun getVariantQueryExpression(): SiloFilterExpression {
        if (expressionStack.size != 1) {
            log.error { "Expected exactly one expression on the stack, but got this stack $expressionStack." }
            throw RuntimeException("Failed to parse variant query.")
        }

        return expressionStack.first()
    }

    override fun enterMetadataQuery(ctx: VariantQueryParser.MetadataQueryContext) {
        val metadataName = ctx.geneOrName(0).text
        val metadataValue = ctx.geneOrName(1).text

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.String -> {
                expressionStack.addLast(StringEquals(metadataName, metadataValue))
            }

            SequenceFilterFieldType.Lineage -> {
                val lineage = LineageEquals(metadataName, metadataValue, false)
                expressionStack.addLast(lineage)
            }

            SequenceFilterFieldType.Boolean -> {
                try {
                    expressionStack.addLast(BooleanEquals(metadataName, metadataValue.toBoolean()))
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("'$metadataValue' is not a valid boolean", e)
                }
            }

            SequenceFilterFieldType.Date -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(metadataName, to = date, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.DateFrom -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(metadataName, to = null, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.DateTo -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(metadataName, to = date, from = null))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            SequenceFilterFieldType.Float -> {
                try {
                    expressionStack.addLast(FloatEquals(metadataName, metadataValue.toDouble()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.FloatFrom -> {
                try {
                    expressionStack.addLast(FloatBetween(metadataName, from = metadataValue.toDouble(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.FloatTo -> {
                try {
                    expressionStack.addLast(FloatBetween(metadataName, to = metadataValue.toDouble(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            SequenceFilterFieldType.Int -> {
                try {
                    expressionStack.addLast(IntEquals(metadataName, metadataValue.toInt()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.IntFrom -> {
                try {
                    expressionStack.addLast(IntBetween(metadataName, from = metadataValue.toInt(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.IntTo -> {
                try {
                    expressionStack.addLast(IntBetween(metadataName, to = metadataValue.toInt(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.StringSearch -> TODO()
            SequenceFilterFieldType.VariantQuery -> {
                throw BadRequestException("VariantQuery cannot be recursive", null)
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

    override fun enterPangolineageQuery(ctx: PangolineageQueryContext) {
        addPangoLineage(ctx.pangolineageWithPossibleSublineages(), PANGO_LINEAGE_COLUMN)
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

    override fun exitNot(ctx: NotContext?) {
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

    override fun exitMaybe(ctx: MaybeContext?) {
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
                value,
                null,
            ),
        )
    }

    override fun enterAaMutationQuery(ctx: AaMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        try {
            referenceGenomeSchema.getGeneFromLowercaseName(ctx.geneOrName().text.lowercase()).name
        } catch (e: RecognitionException) {
            throw BadRequestException("Gene not implemented", e)
        }
        val position = ctx.position().text.toInt()
        val gene = referenceGenomeSchema.getGeneFromLowercaseName(ctx.geneOrName().text.lowercase()).name

        val expression = when (val aaSymbol = ctx.possiblyAmbiguousAaSymbol()) {
            null -> HasAminoAcidMutation(gene, position)
            else -> AminoAcidSymbolEquals(gene, position, aaSymbol.text.uppercase())
        }

        expressionStack.addLast(expression)
    }

    override fun enterAaInsertionQuery(ctx: AaInsertionQueryContext) {
        try {
            referenceGenomeSchema.getGeneFromLowercaseName(ctx.geneOrName().text.lowercase()).name
        } catch (e: RecognitionException) {
            throw BadRequestException("Gene not implemented", e)
        }
        val value = ctx.aaInsertionSymbol().joinToString("", transform = ::mapInsertionSymbol)
        val gene = referenceGenomeSchema.getGeneFromLowercaseName(ctx.geneOrName().text.lowercase()).name

        expressionStack.addLast(
            AminoAcidInsertionContains(
                ctx.position().text.toInt(),
                value,
                gene,
            ),
        )
    }

    override fun enterNextcladePangolineageQuery(ctx: NextcladePangolineageQueryContext) {
        addPangoLineage(ctx.pangolineageWithPossibleSublineages(), NEXTCLADE_PANGO_LINEAGE_COLUMN)
    }

    override fun enterNextstrainCladeQuery(ctx: NextstrainCladeQueryContext) {
        val value = when (ctx.text.uppercase()) {
            NEXTSTRAIN_CLADE_RECOMBINANT -> ctx.text.lowercase()
            else -> ctx.text.uppercase()
        }
        expressionStack.addLast(StringEquals(NEXTSTRAIN_CLADE_COLUMN, value))
    }

    override fun enterGisaidCladeNomenclature(ctx: GisaidCladeNomenclatureContext) {
        expressionStack.addLast(StringEquals(GISAID_CLADE_COLUMN, ctx.text.uppercase()))
    }

    private fun addPangoLineage(
        ctx: PangolineageWithPossibleSublineagesContext,
        pangoLineageColumnName: String,
    ) {
        val pangolineage = ctx.pangolineage().text
        val includeSublineages = ctx.pangolineageIncludeSublineages() != null

        val expr = LineageEquals(pangoLineageColumnName, pangolineage, includeSublineages)
        expressionStack.addLast(expr)
    }
}

fun mapInsertionSymbol(ctx: RuleContext): String =
    when (ctx.text) {
        STOP_CODON -> ESCAPED_STOP_CODON
        LAPIS_INSERTION_AMBIGUITY_SYMBOL -> SILO_INSERTION_AMBIGUITY_SYMBOL
        else -> ctx.text
    }.uppercase()

class SiloNotImplementedError(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause)
