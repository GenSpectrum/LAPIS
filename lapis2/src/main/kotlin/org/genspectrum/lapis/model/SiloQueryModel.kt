package org.genspectrum.lapis.model

import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component

@Component
class SiloQueryModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
) {

    fun aggregate(sequenceFilters: Map<String, String>) = siloClient.sendQuery(
        SiloQuery(
            SiloAction.aggregated(),
            siloFilterExpressionMapper.map(sequenceFilters),
        ),
    )

    fun computeMutationProportions(minProportion: Double?, sequenceFilters: Map<String, String>) = siloClient.sendQuery(
        SiloQuery(
            SiloAction.mutations(minProportion),
            siloFilterExpressionMapper.map(sequenceFilters),
        ),
    )
}
