package org.genspectrum.lapis.model

import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component

@Component
class SiloQueryModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
) {

    fun getAggregated(sequenceFilters: SequenceFiltersRequestWithFields) = siloClient.sendQuery(
        SiloQuery(
            SiloAction.aggregated(
                sequenceFilters.fields,
                sequenceFilters.orderByFields,
                sequenceFilters.limit,
                sequenceFilters.offset,
            ),
            siloFilterExpressionMapper.map(sequenceFilters),
        ),
    )

    fun computeNucleotideMutationProportions(
        sequenceFilters: MutationProportionsRequest,
    ): List<NucleotideMutationResponse> {
        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.mutations(
                    sequenceFilters.minProportion,
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )
        return data.map { it ->
            NucleotideMutationResponse(
                if (
                    it.sequenceName == "main"
                ) {
                    it.mutation
                } else {
                    it.sequenceName + ":" + it.mutation
                },
                it.count,
                it.proportion,
            )
        }
    }

    fun computeAminoAcidMutationProportions(
        sequenceFilters: MutationProportionsRequest,
    ): List<AminoAcidMutationResponse> {
        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.aminoAcidMutations(
                    sequenceFilters.minProportion,
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )
        return data.map { it ->
            AminoAcidMutationResponse(
                it.sequenceName + ":" + it.mutation,
                it.count,
                it.proportion,
            )
        }
    }

    fun getDetails(sequenceFilters: SequenceFiltersRequestWithFields): List<DetailsData> = siloClient.sendQuery(
        SiloQuery(
            SiloAction.details(
                sequenceFilters.fields,
                sequenceFilters.orderByFields,
                sequenceFilters.limit,
                sequenceFilters.offset,
            ),
            siloFilterExpressionMapper.map(sequenceFilters),
        ),
    )
}
