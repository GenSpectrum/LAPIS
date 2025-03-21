package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregatedCollection
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.CsvColumnOrder
import org.genspectrum.lapis.response.DetailsCollection
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.response.SameOrderAsListComparator
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.springframework.stereotype.Component
import java.util.stream.Stream

@Component
class SiloQueryModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    private val databaseConfig: DatabaseConfig,
) {
    fun getAggregated(sequenceFilters: SequenceFiltersRequestWithFields): AggregatedCollection {
        val groupByFields = sequenceFilters.fields.map { it.fieldName }

        val recordStream = siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(
                    groupByFields,
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

        return AggregatedCollection(
            records = recordStream,
            fields = groupByFields,
        )
    }

    fun computeNucleotideMutationProportions(
        sequenceFilters: MutationProportionsRequest,
    ): Stream<NucleotideMutationResponse> {
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
            val mutation = if (referenceGenomeSchema.isSingleSegmented()) {
                it.mutation
            } else {
                "${it.sequenceName}:${it.mutation}"
            }

            NucleotideMutationResponse(
                mutation = mutation,
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = when (referenceGenomeSchema.isSingleSegmented()) {
                    true -> null
                    false -> it.sequenceName
                },
                mutationFrom = it.mutationFrom,
                mutationTo = it.mutationTo,
                position = it.position,
            )
        }
    }

    fun computeAminoAcidMutationProportions(
        sequenceFilters: MutationProportionsRequest,
    ): Stream<AminoAcidMutationResponse> {
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
                mutation = "${it.sequenceName}:${it.mutation}",
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = it.sequenceName,
                mutationFrom = it.mutationFrom,
                mutationTo = it.mutationTo,
                position = it.position,
            )
        }
    }

    fun getDetails(sequenceFilters: SequenceFiltersRequestWithFields): DetailsCollection {
        val fields = sequenceFilters.fields.map { it.fieldName }

        val recordStream = siloClient.sendQuery(
            SiloQuery(
                SiloAction.details(
                    fields,
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

        return DetailsCollection(
            records = recordStream,
            fields = when (fields.isEmpty()) {
                true -> databaseConfig.schema.metadata.map { it.name }
                false -> fields
            },
        )
    }

    fun getNucleotideInsertions(sequenceFilters: SequenceFiltersRequest): Stream<NucleotideInsertionResponse> {
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
            NucleotideInsertionResponse(
                insertion = it.insertion,
                count = it.count,
                insertedSymbols = it.insertedSymbols,
                position = it.position,
                sequenceName = when (referenceGenomeSchema.isSingleSegmented()) {
                    true -> null
                    false -> it.sequenceName
                },
            )
        }
    }

    fun getAminoAcidInsertions(sequenceFilters: SequenceFiltersRequest): Stream<AminoAcidInsertionResponse> {
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
                insertion = it.insertion,
                count = it.count,
                insertedSymbols = it.insertedSymbols,
                position = it.position,
                sequenceName = it.sequenceName,
            )
        }
    }

    fun getGenomicSequence(
        sequenceFilters: SequenceFiltersRequest,
        sequenceType: SequenceType,
        sequenceName: String,
    ) = siloClient.sendQuery(
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
    )

    fun getInfo(): InfoData = siloClient.callInfo()

    fun getLineageDefinition(column: String) = siloClient.getLineageDefinition(column)

    private fun getColumnComparator(csvColumnOrder: CsvColumnOrder) =
        when (csvColumnOrder) {
            CsvColumnOrder.AsInConfig -> SameOrderAsListComparator(databaseConfig.schema.metadata.map { it.name })
            is CsvColumnOrder.AsFieldsInRequest -> SameOrderAsListComparator(csvColumnOrder.fields)
        }
}
