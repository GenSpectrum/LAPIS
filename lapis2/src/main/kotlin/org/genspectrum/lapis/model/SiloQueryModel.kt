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

    fun aggregate(
        sequenceFilters: Map<SequenceFilterFieldName, String>,
        groupByFields: List<SequenceFilterFieldName> = emptyList(),
    ) = siloClient.sendQuery(
        SiloQuery(
            SiloAction.aggregated(groupByFields),
            siloFilterExpressionMapper.map(sequenceFilters),
        ),
    )

    fun computeMutationProportions(minProportion: Double?, sequenceFilters: Map<SequenceFilterFieldName, String>) =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.mutations(minProportion),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )
}
