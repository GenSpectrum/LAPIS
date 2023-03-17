package org.genspectrum.lapis.model

import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.springframework.stereotype.Component

@Component
class AggregatedModel(private val siloClient: SiloClient) {
    fun handleRequest(filterParameter: Map<String, String>): AggregatedResponse {
        if (filterParameter.isEmpty()) {
            return siloClient.sendQuery(
                SiloQuery(SiloAction.aggregated(), True),
            )
        }

        return siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(),
                And(
                    filterParameter.map { convertToSiloFilter(it.key, it.value) },
                ),
            ),
        )
    }

    fun convertToSiloFilter(key: String, value: String): SiloFilterExpression {
        if (key == "pangoLineage") {
            return PangoLineageEquals(value)
        }
        return StringEquals(key, value)
    }
}
