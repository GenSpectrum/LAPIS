package org.genspectrum.lapis.model

import VariantQueryBaseListener
import VariantQueryParser
import VariantQueryParser.AaInsertionQueryContext
import VariantQueryParser.AaMutationQueryContext
import VariantQueryParser.AndContext
import VariantQueryParser.MaybeContext
import VariantQueryParser.NOfQueryContext
import VariantQueryParser.NextcladePangolineageQueryContext
import VariantQueryParser.NextstrainCladeQueryContext
import VariantQueryParser.NotContext
import VariantQueryParser.NucleotideInsertionQueryContext
import VariantQueryParser.NucleotideMutationQueryContext
import VariantQueryParser.OrContext
import VariantQueryParser.PangolineageQueryContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.request.LAPIS_INSERTION_AMBIGUITY_SYMBOL
import org.genspectrum.lapis.request.SILO_INSERTION_AMBIGUITY_SYMBOL
import org.genspectrum.lapis.silo.AminoAcidInsertionContains
import org.genspectrum.lapis.silo.AminoAcidSymbolEquals
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.HasAminoAcidMutation
import org.genspectrum.lapis.silo.HasNucleotideMutation
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideInsertionContains
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.StringEquals

class VariantQueryCustomListener : VariantQueryBaseListener(), ParseTreeListener {
    private val expressionStack = ArrayDeque<SiloFilterExpression>()

    fun getVariantQueryExpression(): SiloFilterExpression {
        return expressionStack.first()
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
        addPangoLineage(ctx, PANGO_LINEAGE_COLUMN)
    }

    override fun exitAnd(ctx: AndContext?) {
        val children = listOf(expressionStack.removeLast(), expressionStack.removeLast()).reversed()
        expressionStack.addLast(And(children))
    }

    override fun exitNot(ctx: NotContext?) {
        val child = expressionStack.removeLast()
        expressionStack.addLast(Not(child))
    }

    override fun exitOr(ctx: OrContext?) {
        val children = listOf(expressionStack.removeLast(), expressionStack.removeLast()).reversed()
        expressionStack.addLast(Or(children))
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

        val children = mutableListOf<SiloFilterExpression>()
        for (i in 1..n) {
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
            ),
        )
    }

    override fun enterAaMutationQuery(ctx: AaMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        val position = ctx.position().text.toInt()

        val expression = when (val aaSymbol = ctx.possiblyAmbiguousAaSymbol()) {
            null -> HasAminoAcidMutation(ctx.gene().text, position)
            else -> AminoAcidSymbolEquals(ctx.gene().text, position, aaSymbol.text.uppercase())
        }

        expressionStack.addLast(expression)
    }

    override fun enterAaInsertionQuery(ctx: AaInsertionQueryContext) {
        val value = ctx.aaInsertionSymbol().joinToString("", transform = ::mapInsertionSymbol)
        expressionStack.addLast(
            AminoAcidInsertionContains(
                ctx.position().text.toInt(),
                value.uppercase(),
                ctx.gene().text,
            ),
        )
    }

    override fun enterNextcladePangolineageQuery(ctx: NextcladePangolineageQueryContext) {
        addPangoLineage(ctx.pangolineageQuery(), NEXTCLADE_PANGO_LINEAGE_COLUMN)
    }

    override fun enterNextstrainCladeQuery(ctx: NextstrainCladeQueryContext) {
        val value = when (ctx.text) {
            NEXTSTRAIN_CLADE_RECOMBINANT -> ctx.text.lowercase()
            else -> ctx.text.uppercase()
        }
        expressionStack.addLast(StringEquals(NEXTSTRAIN_CLADE_COLUMN, value))
    }

    override fun enterGisaidCladeNomenclature(ctx: VariantQueryParser.GisaidCladeNomenclatureContext) {
        expressionStack.addLast(StringEquals(GISAID_CLADE_COLUMN, ctx.text.uppercase()))
    }

    private fun addPangoLineage(
        ctx: PangolineageQueryContext,
        pangoLineageColumnName: String,
    ) {
        val pangolineage = ctx.pangolineage().text
        val includeSublineages = ctx.pangolineageIncludeSublineages() != null

        val expr = PangoLineageEquals(pangoLineageColumnName, pangolineage, includeSublineages)
        expressionStack.addLast(expr)
    }
}

fun mapInsertionSymbol(ctx: RuleContext): String =
    when (ctx.text) {
        LAPIS_INSERTION_AMBIGUITY_SYMBOL -> SILO_INSERTION_AMBIGUITY_SYMBOL
        else -> ctx.text
    }

class SiloNotImplementedError(message: String?, cause: Throwable?) : Exception(message, cause)
