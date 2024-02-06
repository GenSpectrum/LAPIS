package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component

@Component
class SiloQueryModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
    fun getAggregated(sequenceFilters: SequenceFiltersRequestWithFields) =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(
                    sequenceFilters.fields.map { it.fieldName },
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
        return data.map {
            val sequenceName =
                if (referenceGenomeSchema.isSingleSegmented()) it.mutation else "${it.sequenceName}:${it.mutation}"

            NucleotideMutationResponse(
                sequenceName,
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
        return data.map {
            AminoAcidMutationResponse(
                "${it.sequenceName}:${it.mutation}",
                it.count,
                it.proportion,
            )
        }
    }

    fun getDetails(sequenceFilters: SequenceFiltersRequestWithFields): List<DetailsData> =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.details(
                    sequenceFilters.fields.map { it.fieldName },
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun getNucleotideInsertions(sequenceFilters: SequenceFiltersRequest): List<NucleotideInsertionResponse> {
        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.nucleotideInsertions(
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

        return data.map {
            val sequenceName = if (referenceGenomeSchema.isSingleSegmented()) "" else "${it.sequenceName}:"

            NucleotideInsertionResponse(
                "ins_${sequenceName}${it.position}:${it.insertions}",
                it.count,
            )
        }
    }

    fun getAminoAcidInsertions(sequenceFilters: SequenceFiltersRequest): List<AminoAcidInsertionResponse> {
        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.aminoAcidInsertions(
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

        return data.map {
            AminoAcidInsertionResponse(
                "ins_${it.sequenceName}:${it.position}:${it.insertions}",
                it.count,
            )
        }
    }

    fun getGenomicSequence(
        sequenceFilters: SequenceFiltersRequest,
        sequenceType: SequenceType,
        sequenceName: String,
    ): String {
        return siloClient.sendQuery(
            SiloQuery(
                SiloAction.genomicSequence(
                    sequenceType,
                    sequenceName,
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        ).joinToString("\n") { ">${it.sequenceKey}\n${it.sequence}" }
    }

    fun getInfo(): InfoData = siloClient.callInfo()
}
