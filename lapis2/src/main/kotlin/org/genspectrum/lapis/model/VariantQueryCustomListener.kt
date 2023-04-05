package org.genspectrum.lapis.model

import VariantQueryBaseListener
import VariantQueryParser.AndContext
import VariantQueryParser.MaybeContext
import VariantQueryParser.N_of_queryContext
import VariantQueryParser.NotContext
import VariantQueryParser.Nucleotide_mutationContext
import VariantQueryParser.OrContext
import VariantQueryParser.Pangolineage_queryContext
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
    private var expressionStack = ArrayDeque<SiloFilterExpression>()

    fun getExpr(): SiloFilterExpression {
        return expressionStack.first()
    }

    override fun enterNucleotide_mutation(ctx: Nucleotide_mutationContext?) {
        if (ctx == null) {
            return
        }
        val position = ctx.position().text.toInt()
        val secondSymbol = ctx.ambigous_nucleotide_symbol()?.text ?: "-"

        val expr = NucleotideSymbolEquals(position, secondSymbol)
        expressionStack.addLast(expr)
    }

    override fun enterPangolineage_query(ctx: Pangolineage_queryContext?) {
        if (ctx == null) {
            return
        }
        val pangolineage = ctx.pangolineage().text
        val includeSublineages = ctx.pangolineage_include_sublineages() != null

        val expr = PangoLineageEquals(pangolineage, includeSublineages)
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

    override fun exitN_of_query(ctx: N_of_queryContext?) {
        if (ctx == null) {
            return
        }

        val n = ctx.n_of_number_of_matchers().text.toInt()
        val matchExactly = ctx.n_of_match_exactly() != null

        val children = mutableListOf<SiloFilterExpression>()
        for (i in 1..n) {
            children += expressionStack.removeLast()
        }

        expressionStack.addLast(NOf(n, matchExactly, children.reversed()))
    }
}
