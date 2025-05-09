package org.genspectrum.lapis.model

import AdvancedQueryLexer
import AdvancedQueryParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.BadRequestException
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.springframework.stereotype.Component

@Component
class AdvancedQueryFacade(
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    private val databaseConfig: DatabaseConfig,
) {
    fun map(advancedQuery: String): SiloFilterExpression {
        val lexer = AdvancedQueryLexer(CharStreams.fromString(advancedQuery))
        val tokens = CommonTokenStream(lexer)
        val parser = AdvancedQueryParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(ThrowingAdvancedQueryErrorListener())

        val listener = AdvancedQueryCustomListener(referenceGenomeSchema, databaseConfig)
        val walker = ParseTreeWalker()
        walker.walk(listener, parser.start())

        return listener.getAdvancedQueryExpression()
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
    ): Unit = throw BadRequestException("Failed to parse advanced query (line $line:$charPositionInLine): $message.")
}
