package org.genspectrum.lapis.model

import VariantQueryBaseListener
import VariantQueryParser.AaInsertionQueryContext
import VariantQueryParser.AaMutationQueryContext
import VariantQueryParser.AndContext
import VariantQueryParser.GisaidCladeLineageQueryContext
import VariantQueryParser.MaybeContext
import VariantQueryParser.NOfQueryContext
import VariantQueryParser.NextcladePangolineageQueryContext
import VariantQueryParser.NextstrainCladeQueryContext
import VariantQueryParser.NotContext
import VariantQueryParser.NucleotideInsertionQueryContext
import VariantQueryParser.NucleotideMutationQueryContext
import VariantQueryParser.OrContext
import VariantQueryParser.PangolineageQueryContext
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.Maybe
import org.genspectrum.lapis.silo.NOf
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.Or
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloFilterExpression

class VariantQueryCustomListener : VariantQueryBaseListener(), ParseTreeListener {
    private val expressionStack = ArrayDeque<SiloFilterExpression>()

    fun getExpr(): SiloFilterExpression {
        return expressionStack.first()
    }

    override fun enterNucleotideMutationQuery(ctx: NucleotideMutationQueryContext?) {
        if (ctx == null) {
            return
        }
        val position = ctx.position().text.toInt()
        val secondSymbol = ctx.nucleotideMutationQuerySecondSymbol()?.text ?: "-"

        val expr = NucleotideSymbolEquals(position, secondSymbol)
        expressionStack.addLast(expr)
    }

    override fun enterPangolineageQuery(ctx: PangolineageQueryContext?) {
        if (ctx == null) {
            return
        }
        val pangolineage = ctx.pangolineage().text
        val includeSublineages = ctx.pangolineageIncludeSublineages() != null

        val expr = PangoLineageEquals("pangoLineage", pangolineage, includeSublineages)
        expressionStack.addLast(expr)
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

    override fun enterNucleotideInsertionQuery(ctx: NucleotideInsertionQueryContext?) {
        throw NotImplementedError("Nucleotide insertions are not supported yet.")
    }

    override fun enterAaMutationQuery(ctx: AaMutationQueryContext?) {
        throw NotImplementedError("Amino acid mutations are not supported yet.")
    }

    override fun enterAaInsertionQuery(ctx: AaInsertionQueryContext?) {
        throw NotImplementedError("Amino acid insertions are not supported yet.")
    }

    override fun enterNextcladePangolineageQuery(ctx: NextcladePangolineageQueryContext?) {
        throw NotImplementedError("Nextclade pango lineages are not supported yet.")
    }

    override fun enterNextstrainCladeQuery(ctx: NextstrainCladeQueryContext?) {
        throw NotImplementedError("Nextstrain clade lineages are not supported yet.")
    }

    override fun enterGisaidCladeLineageQuery(ctx: GisaidCladeLineageQueryContext?) {
        throw NotImplementedError("Gisaid clade lineages are not supported yet.")
    }
}
