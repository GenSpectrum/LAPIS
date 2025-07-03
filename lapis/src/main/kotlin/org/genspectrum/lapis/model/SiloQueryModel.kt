package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.response.SequenceData
import org.genspectrum.lapis.silo.SequenceType
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.util.toUnalignedSequenceName
import org.springframework.stereotype.Component
import java.util.stream.Stream

@Component
class SiloQueryModel(
    private val siloClient: SiloClient,
    private val siloFilterExpressionMapper: SiloFilterExpressionMapper,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    private val fastaHeaderTemplateParser: FastaHeaderTemplateParser,
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

    fun computeNucleotideMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val fields = sequenceFilters.fields
            .let {
                when {
                    it.contains(MutationsField.MUTATION) -> addSequenceNameIfMissing(it)
                    else -> it
                }
            }
            .map { it.value }

        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.mutations(
                    minProportion = sequenceFilters.minProportion,
                    orderByFields = sequenceFilters.orderByFields,
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                    fields = fields,
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

            MutationResponse(
                mutation = mutation,
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = if (!sequenceFilters.shouldResponseContainSequenceName()) {
                    null
                } else if (referenceGenomeSchema.isSingleSegmented()) {
                    ExplicitlyNullable(null)
                } else {
                    ExplicitlyNullable(it.sequenceName)
                },
                mutationFrom = it.mutationFrom,
                mutationTo = it.mutationTo,
                position = it.position,
            )
        }
    }

    fun computeAminoAcidMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val fields = sequenceFilters.fields
            .let {
                when {
                    it.contains(MutationsField.MUTATION) -> addSequenceNameIfMissing(it)
                    else -> it
                }
            }
            .map { it.value }

        val data = siloClient.sendQuery(
            SiloQuery(
                SiloAction.aminoAcidMutations(
                    minProportion = sequenceFilters.minProportion,
                    orderByFields = sequenceFilters.orderByFields,
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                    fields = fields,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )
        return data.map {
            MutationResponse(
                mutation = "${it.sequenceName}:${it.mutation}",
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = if (!sequenceFilters.shouldResponseContainSequenceName()) {
                    null
                } else {
                    ExplicitlyNullable(it.sequenceName)
                },
                mutationFrom = it.mutationFrom,
                mutationTo = it.mutationTo,
                position = it.position,
            )
        }
    }

    fun getDetails(sequenceFilters: SequenceFiltersRequestWithFields) =
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

    fun getNucleotideInsertions(sequenceFilters: SequenceFiltersRequest): Stream<InsertionResponse> {
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
            InsertionResponse(
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

    fun getAminoAcidInsertions(sequenceFilters: SequenceFiltersRequest): Stream<InsertionResponse> {
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
            InsertionResponse(
                insertion = it.insertion,
                count = it.count,
                insertedSymbols = it.insertedSymbols,
                position = it.position,
                sequenceName = it.sequenceName,
            )
        }
    }

    fun getGenomicSequence(
        sequenceFilters: CommonSequenceFilters,
        sequenceType: SequenceType,
        sequenceNames: List<String>,
        rawFastaHeaderTemplate: String,
    ): SequencesResponse {
        val fastaHeaderTemplate = fastaHeaderTemplateParser.parseTemplate(rawFastaHeaderTemplate)

        // TODO

        val sequenceData = siloClient.sendQuery(
            SiloQuery(
                SiloAction.genomicSequence(
                    type = sequenceType,
                    sequenceNames = mapSequenceNames(sequenceNames, sequenceType),
                    orderByFields = mapSequenceOrderByFields(sequenceFilters.orderByFields, sequenceType),
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )
        return SequencesResponse(
            sequenceData = sequenceData,
            requestedSequenceNames = sequenceNames,
            fastaHeaderTemplate = fastaHeaderTemplate,
        )
    }

    private fun mapSequenceNames(
        sequenceNames: List<String>,
        sequenceType: SequenceType,
    ) = sequenceNames
        .map { referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(it) ?: it }
        .map {
            when (sequenceType) {
                SequenceType.ALIGNED -> it
                SequenceType.UNALIGNED -> toUnalignedSequenceName(it)
            }
        }

    private fun mapSequenceOrderByFields(
        orderByFields: List<OrderByField>,
        sequenceType: SequenceType,
    ) = orderByFields
        .map { it.copy(field = referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(it.field) ?: it.field) }
        .map {
            when (sequenceType) {
                SequenceType.ALIGNED -> it
                SequenceType.UNALIGNED -> {
                    when (val sequenceName = referenceGenomeSchema.getNucleotideSequence(it.field)) {
                        null -> it
                        else -> it.copy(field = toUnalignedSequenceName(sequenceName.name))
                    }
                }
            }
        }

    fun getInfo(): InfoData = siloClient.callInfo()

    fun getLineageDefinition(column: String) = siloClient.getLineageDefinition(column)

    private fun addSequenceNameIfMissing(fields: List<MutationsField>) =
        when {
            !fields.contains(MutationsField.SEQUENCE_NAME) -> fields + MutationsField.SEQUENCE_NAME
            else -> fields
        }
}

data class SequencesResponse(
    val sequenceData: Stream<SequenceData>,
    val requestedSequenceNames: List<String>,
    val fastaHeaderTemplate: FastaHeaderTemplate,
)
