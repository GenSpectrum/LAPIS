package org.genspectrum.lapis.model

import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component

@Component
class AggregatedModel(private val siloClient: SiloClient, private val queryMapper: QueryMapper) {

    fun handleRequest(sequenceFilters: Map<String, String>) = siloClient.sendQuery(
        SiloQuery(
            SiloAction.aggregated(),
            queryMapper.map(sequenceFilters),
        ),
    )
}
