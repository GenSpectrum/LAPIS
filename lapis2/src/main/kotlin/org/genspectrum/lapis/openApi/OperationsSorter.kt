package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

@Component
class OperationsSorter : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        operation.responses.forEach { _, response ->
            val applicationJsonFirstContents =
                response.content.toSortedMap(
                    compareByDescending<String> { it.contains(MediaType.APPLICATION_JSON_VALUE) }.thenBy { it },
                )

            response.content = Content().apply {
                for ((mediaTypeName, mediaType) in applicationJsonFirstContents) {
                    addMediaType(mediaTypeName, mediaType)
                }
            }
        }

        return operation
    }
}
