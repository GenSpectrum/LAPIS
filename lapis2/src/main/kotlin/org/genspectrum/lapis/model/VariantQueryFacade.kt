package org.genspectrum.lapis.model

import VariantQueryLexer
import VariantQueryParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.springframework.stereotype.Component

@Component
class VariantQueryFacade(val referenceGenomeSchema: ReferenceGenomeSchema) {
    fun map(variantQuery: String): SiloFilterExpression {
        val lexer = VariantQueryLexer(CharStreams.fromString(variantQuery))
        val tokens = CommonTokenStream(lexer)
        val parser = VariantQueryParser(tokens)
        val listener = VariantQueryCustomListener(referenceGenomeSchema)

        val walker = ParseTreeWalker()
        walker.walk(listener, parser.start())

        return listener.getVariantQueryExpression()
    }
}
