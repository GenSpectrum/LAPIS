package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter
import org.genspectrum.lapis.controller.INFO_ROUTE
import org.genspectrum.lapis.request.COMPRESSION_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_AS_FILE_PROPERTY
import org.genspectrum.lapis.request.DOWNLOAD_FILE_BASENAME_PROPERTY
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class SampleEndpointsGetParameterCustomizer : OpenApiCustomizer {
    companion object {
        private val PATH_WITHOUT_DOWNLOAD_AS_FILE = listOf(
            "/sample$INFO_ROUTE",
        )
    }

    override fun customise(openApi: OpenAPI) {
        for ((_, path) in openApi.paths.filter { (url, _) ->
            url.startsWith("/sample") &&
                !PATH_WITHOUT_DOWNLOAD_AS_FILE.any {
                    url.startsWith(it)
                }
        }) {
            path.get.addParametersItem(
                Parameter()
                    .`in`("query")
                    .name(DOWNLOAD_AS_FILE_PROPERTY)
                    .description(DOWNLOAD_AS_FILE_DESCRIPTION)
                    .schema(downloadAsFileSchema()),
            )
            path.get.addParametersItem(
                Parameter()
                    .`in`("query")
                    .name(DOWNLOAD_FILE_BASENAME_PROPERTY)
                    .description(DOWNLOAD_FILE_BASENAME_DESCRIPTION)
                    .schema(downloadFileBasenameSchema()),
            )
            path.get.addParametersItem(
                Parameter()
                    .`in`("query")
                    .name(COMPRESSION_PROPERTY)
                    .description(COMPRESSION_DESCRIPTION)
                    .schema(compressionSchema()),
            )
        }
    }
}
