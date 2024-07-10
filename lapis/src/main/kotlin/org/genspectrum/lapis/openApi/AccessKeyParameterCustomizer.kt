package org.genspectrum.lapis.openApi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.parameters.Parameter
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.OpennessLevel
import org.genspectrum.lapis.controller.INFO_ROUTE
import org.genspectrum.lapis.request.ACCESS_KEY_PROPERTY
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class AccessKeyParameterCustomizer(
    private val databaseConfig: DatabaseConfig,
) : OpenApiCustomizer {
    companion object {
        private val PATH_WITH_ACCESS_KEY_PARAMETER = listOf(
            "/sample$INFO_ROUTE",
        )
    }

    override fun customise(openApi: OpenAPI) {
        if (databaseConfig.schema.opennessLevel == OpennessLevel.OPEN) {
            return
        }

        for ((_, path) in openApi.paths.filter { (url, _) ->
            PATH_WITH_ACCESS_KEY_PARAMETER.any {
                url.startsWith(it)
            }
        }) {
            path.get.addParametersItem(
                Parameter()
                    .`in`("query")
                    .name(ACCESS_KEY_PROPERTY)
                    .description(ACCESS_KEY_DESCRIPTION)
                    .schema(accessKeySchema()),
            )
        }
    }
}
