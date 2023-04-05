package org.genspectrum.lapis.model

import VariantQueryBaseListener
import VariantQueryParser.AndContext
import VariantQueryParser.NotContext
import VariantQueryParser.Nucleotide_mutationContext
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.Not
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
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
        val secondSymbol = if (ctx.ambigous_nucleotide_symbol() != null) ctx.ambigous_nucleotide_symbol().text else "-"

        val expr = NucleotideSymbolEquals(position, secondSymbol)
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
}
