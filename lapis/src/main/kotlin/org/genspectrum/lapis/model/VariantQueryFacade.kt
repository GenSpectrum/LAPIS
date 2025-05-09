package org.genspectrum.lapis.model

import VariantQueryLexer
import VariantQueryParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.springframework.stereotype.Component

@Component
class VariantQueryFacade(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
    fun map(variantQuery: String): SiloFilterExpression {
        val lexer = VariantQueryLexer(CharStreams.fromString(variantQuery))
        val tokens = CommonTokenStream(lexer)
        val parser = VariantQueryParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingErrorListener())

        val listener = VariantQueryCustomListener(referenceGenomeSchema)

        val walker = ParseTreeWalker()
        walker.walk(listener, parser.start())

        return listener.getVariantQueryExpression()
    }
}

class ThrowingErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        message: String?,
        exception: RecognitionException?,
    ): Unit = throw BadRequestException("Failed to parse variant query (line $line:$charPositionInLine): $message.")
}
