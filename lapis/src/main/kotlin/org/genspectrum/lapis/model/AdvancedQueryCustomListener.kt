package org.genspectrum.lapis.model

import AdvancedQueryBaseListener
import AdvancedQueryParser.AaInsertionQueryContext
import AdvancedQueryParser.AaMutationQueryContext
import AdvancedQueryParser.AndContext
import AdvancedQueryParser.MaybeContext
import AdvancedQueryParser.NOfQueryContext
import AdvancedQueryParser.NotContext
import AdvancedQueryParser.NucleotideInsertionQueryContext
import AdvancedQueryParser.NucleotideMutationQueryContext
import AdvancedQueryParser.OrContext
import mu.KotlinLogging
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.config.SequenceFilterField
import org.genspectrum.lapis.config.SequenceFilterFieldType
import org.genspectrum.lapis.config.SequenceFilterFields
import org.genspectrum.lapis.controller.BadRequestException
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

private val log = KotlinLogging.logger { }

class AdvancedQueryCustomListener(
    val referenceGenomeSchema: ReferenceGenomeSchema,
    private val allowedSequenceFilterFields: SequenceFilterFields,
) : AdvancedQueryBaseListener(),
    ParseTreeListener {
    private val expressionStack = ArrayDeque<SiloFilterExpression>()

    fun getAdvancedQueryExpression(): SiloFilterExpression {
        if (expressionStack.size != 1) {
            log.error { "Expected exactly one expression on the stack, but got this stack $expressionStack." }
            throw RuntimeException("Failed to parse advanced query.")
        }

        return expressionStack.first()
    }

    override fun enterMetadataLessThanEqualQuery(ctx: AdvancedQueryParser.MetadataLessThanEqualQueryContext) {
        val metadataName = ctx.geneOrName()[0].text
        val metadataValue = ctx.geneOrName()[1].text

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.Date -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = date, from = null))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.Float -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, to = metadataValue.toDouble(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.Int -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, to = metadataValue.toInt(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            else -> {
                throw BadRequestException("expression <= can not be used for ${field.type}", null)
            }
        }
    }

    override fun enterMetadataGreaterThanEqualQuery(ctx: AdvancedQueryParser.MetadataGreaterThanEqualQueryContext) {
        val metadataName = ctx.geneOrName()[0].text
        val metadataValue = ctx.geneOrName()[1].text

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.Date -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = null, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.Float -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, to = metadataValue.toDouble(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.Int -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, to = metadataValue.toInt(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            else -> {
                throw BadRequestException("expression >= can not be used for ${field.type}", null)
            }
        }
    }

    private fun mapToLineageFilter(
        metadataName: String,
        metadataValue: String?,
    ): LineageEquals {
        val sanitizedValue = metadataValue?.removeSurrounding("'") // Remove surrounding single quotes

        return when {
            sanitizedValue.isNullOrBlank() -> LineageEquals(metadataName, null, includeSublineages = false)
            sanitizedValue.endsWith(
                ".*",
            ) -> LineageEquals(metadataName, sanitizedValue.substringBeforeLast(".*"), includeSublineages = true)
            sanitizedValue.endsWith(
                '*',
            ) -> LineageEquals(metadataName, sanitizedValue.substringBeforeLast('*'), includeSublineages = true)
            sanitizedValue.endsWith('.') -> throw BadRequestException(
                "Invalid lineage: $sanitizedValue must not end with a dot. Did you mean '$sanitizedValue*'?",
            )
            else -> LineageEquals(metadataName, sanitizedValue, includeSublineages = false)
        }
    }

    override fun enterMetadataQuery(ctx: AdvancedQueryParser.MetadataQueryContext) {
        val metadataName = ctx.geneOrName()[0].text
        val metadataValue = ctx.geneOrName()[1].text

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.String -> {
                expressionStack.addLast(StringEquals(field.name, metadataValue))
            }

            SequenceFilterFieldType.Lineage -> {
                val lineage = mapToLineageFilter(
                    field.name,
                    metadataValue,
                )
                expressionStack.addLast(lineage)
            }

            SequenceFilterFieldType.Boolean -> {
                try {
                    expressionStack.addLast(BooleanEquals(field.name, metadataValue.toBoolean()))
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("'$metadataValue' is not a valid boolean", e)
                }
            }

            SequenceFilterFieldType.Date -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = date, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.DateFrom -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = null, from = date))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            is SequenceFilterFieldType.DateTo -> {
                try {
                    val date = LocalDate.parse(metadataValue)
                    expressionStack.addLast(DateBetween(field.name, to = date, from = null))
                } catch (exception: DateTimeParseException) {
                    throw BadRequestException("'$metadataValue' is not a valid date: ${exception.message}", exception)
                }
            }

            SequenceFilterFieldType.Float -> {
                try {
                    expressionStack.addLast(FloatEquals(field.name, metadataValue.toDouble()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.FloatFrom -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, from = metadataValue.toDouble(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            is SequenceFilterFieldType.FloatTo -> {
                try {
                    expressionStack.addLast(FloatBetween(field.name, to = metadataValue.toDouble(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid float", e)
                }
            }

            SequenceFilterFieldType.Int -> {
                try {
                    expressionStack.addLast(IntEquals(field.name, metadataValue.toInt()))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.IntFrom -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, from = metadataValue.toInt(), to = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.IntTo -> {
                try {
                    expressionStack.addLast(IntBetween(field.name, to = metadataValue.toInt(), from = null))
                } catch (e: NumberFormatException) {
                    throw BadRequestException("'$metadataValue' is not a valid integer", e)
                }
            }

            is SequenceFilterFieldType.StringSearch -> {
                expressionStack.addLast(StringSearch(field.name, metadataValue))
            }

            SequenceFilterFieldType.VariantQuery -> {
                throw BadRequestException("VariantQuery cannot be called from advanced query", null)
            }

            SequenceFilterFieldType.AdvancedQuery -> {
                throw BadRequestException("AdvancedQuery cannot be called recursively", null)
            }
        }
    }

    override fun enterRegexMetadataQuery(ctx: AdvancedQueryParser.RegexMetadataQueryContext) {
        val metadataName = ctx.geneOrName().text
        val metadataValue = ctx.value().text
        val stripped = metadataValue.trim('\'')

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.String -> {
                expressionStack.addLast(StringEquals(field.name, stripped))
            }

            is SequenceFilterFieldType.StringSearch -> {
                expressionStack.addLast(StringSearch(field.name, stripped))
            }

            SequenceFilterFieldType.VariantQuery -> {
                throw BadRequestException("VariantQuery cannot be called from advanced query", null)
            }

            SequenceFilterFieldType.AdvancedQuery -> {
                throw BadRequestException("AdvancedQuery cannot be called recursively", null)
            }

            else -> {
                throw BadRequestException(
                    "Expression contains symbols not allowed for metadata field of type ${field.type} (allowed symbols: a-z, A-Z, 0-9, ., *, -, _)",
                    null,
                )
            }
        }
    }

    override fun enterIsNullQuery(ctx: AdvancedQueryParser.IsNullQueryContext) {
        val metadataName = ctx.geneOrName().text

        val field: SequenceFilterField? = allowedSequenceFilterFields.fields[metadataName.lowercase(Locale.US)]
        field ?: throw BadRequestException("Metadata field $metadataName does not exist", null)
        when (field.type) {
            SequenceFilterFieldType.String -> {
                expressionStack.addLast(StringEquals(field.name, null))
            }

            SequenceFilterFieldType.Lineage -> {
                val lineage = LineageEquals(field.name, null, false)
                expressionStack.addLast(lineage)
            }

            SequenceFilterFieldType.Boolean -> {
                expressionStack.addLast(BooleanEquals(field.name, null))
            }

            SequenceFilterFieldType.Date -> {
                expressionStack.addLast(Not(DateBetween(field.name, to = null, from = null)))
            }

            is SequenceFilterFieldType.DateFrom -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            is SequenceFilterFieldType.DateTo -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            SequenceFilterFieldType.Float -> {
                expressionStack.addLast(FloatEquals(field.name, null))
            }

            is SequenceFilterFieldType.FloatFrom -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            is SequenceFilterFieldType.FloatTo -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            SequenceFilterFieldType.Int -> {
                expressionStack.addLast(IntEquals(field.name, null))
            }

            is SequenceFilterFieldType.IntFrom -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            is SequenceFilterFieldType.IntTo -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            is SequenceFilterFieldType.StringSearch -> {
                val fieldName = field.name.split(".").first()
                throw BadRequestException("Filter IsNull($fieldName) instead of IsNull($metadataName)", null)
            }

            SequenceFilterFieldType.VariantQuery -> {
                throw BadRequestException("VariantQuery cannot be called from advanced query", null)
            }

            SequenceFilterFieldType.AdvancedQuery -> {
                throw BadRequestException("AdvancedQuery cannot be called recursively", null)
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
                value.uppercase(),
                null,
            ),
        )
    }

    override fun enterAaMutationQuery(ctx: AaMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        val position = ctx.position().text.toInt()
        val gene = referenceGenomeSchema.getGene(ctx.geneOrName().text).name

        val expression = when (val aaSymbol = ctx.possiblyAmbiguousAaSymbol()) {
            null -> HasAminoAcidMutation(gene, position)
            else -> AminoAcidSymbolEquals(gene, position, aaSymbol.text.uppercase())
        }

        expressionStack.addLast(expression)
    }

    override fun enterAaInsertionQuery(ctx: AaInsertionQueryContext) {
        val value = ctx.aaInsertionSymbol().joinToString("", transform = ::mapInsertionSymbol)
        val gene = referenceGenomeSchema.getGene(ctx.geneOrName().text).name

        expressionStack.addLast(
            AminoAcidInsertionContains(
                ctx.position().text.toInt(),
                value.uppercase(),
                gene,
            ),
        )
    }
}
