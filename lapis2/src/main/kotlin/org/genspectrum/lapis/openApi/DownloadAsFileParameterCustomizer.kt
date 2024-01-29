package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter
import org.genspectrum.lapis.controller.DOWNLOAD_AS_FILE_PROPERTY
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class DownloadAsFileParameterCustomizer : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        for ((_, path) in openApi.paths.filter { (url, _) -> url.startsWith("/sample") }) {
            path.get.parameters.add(
                Parameter()
                    .`in`("query")
                    .name(DOWNLOAD_AS_FILE_PROPERTY)
                    .description(DOWNLOAD_AS_FILE_DESCRIPTION)
                    .schema(downloadAsFileSchema()),
            )
        }
    }
}
