package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.AggregatedFiltersRequest
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.DetailsFiltersRequest
import org.genspectrum.lapis.request.MRCASequenceFiltersRequest
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.OrderBySpec
import org.genspectrum.lapis.request.PhyloTreeSequenceFiltersRequest
import org.genspectrum.lapis.request.PlainField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequencePositionField
import org.genspectrum.lapis.response.ExplicitlyNullable
import org.genspectrum.lapis.response.InfoData
import org.genspectrum.lapis.response.InsertionResponse
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.MutationResponse
import org.genspectrum.lapis.response.PhyloSubtreeData
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
    databaseConfig: DatabaseConfig,
) {
    private val allMetadataFields = databaseConfig.schema.metadata.map { it.name }

    fun getAggregated(sequenceFilters: AggregatedFiltersRequest) =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(
                    groupByFields = sequenceFilters.fields.filterIsInstance<PlainField>().map { it.fieldName },
                    orderByFields = sequenceFilters.orderByFields,
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                    sequencePositionFields = sequenceFilters.fields.filterIsInstance<SequencePositionField>(),
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun computeNucleotideMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val fields = siloMutationFields(sequenceFilters.fields)

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
            val mutation = if (sequenceFilters.shouldResponseContainField(MutationsField.MUTATION)) {
                val core = "${it.mutationFrom}${it.position}${it.mutationTo}"
                if (referenceGenomeSchema.isSingleSegmented()) {
                    core
                } else {
                    "${it.sequenceName}:$core"
                }
            } else {
                null
            }

            MutationResponse(
                mutation = mutation,
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = if (!sequenceFilters.shouldResponseContainField(MutationsField.SEQUENCE_NAME)) {
                    null
                } else if (referenceGenomeSchema.isSingleSegmented()) {
                    ExplicitlyNullable(null)
                } else {
                    ExplicitlyNullable(it.sequenceName)
                },
                mutationFrom = mutationFromOrNull(sequenceFilters, it),
                mutationTo = mutationToOrNull(sequenceFilters, it),
                position = positionOrNull(sequenceFilters, it),
            )
        }
    }

    fun computeAminoAcidMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val fields = siloMutationFields(sequenceFilters.fields)

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
                mutation = if (sequenceFilters.shouldResponseContainField(MutationsField.MUTATION)) {
                    "${it.sequenceName}:${it.mutationFrom}${it.position}${it.mutationTo}"
                } else {
                    null
                },
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = if (!sequenceFilters.shouldResponseContainField(MutationsField.SEQUENCE_NAME)) {
                    null
                } else {
                    ExplicitlyNullable(it.sequenceName)
                },
                mutationFrom = mutationFromOrNull(sequenceFilters, it),
                mutationTo = mutationToOrNull(sequenceFilters, it),
                position = positionOrNull(sequenceFilters, it),
            )
        }
    }

    fun getDetails(sequenceFilters: DetailsFiltersRequest) =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.details(
                    sequenceFilters.fields.map { it.fieldName }.ifEmpty { allMetadataFields },
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun getMostRecentCommonAncestor(sequenceFilters: MRCASequenceFiltersRequest) =
        siloClient.sendQuery(
            SiloQuery(
                SiloAction.mostRecentCommonAncestor(
                    sequenceFilters.phyloTreeField,
                    sequenceFilters.printNodesNotInTree,
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
            val sequenceName = when (referenceGenomeSchema.isSingleSegmented()) {
                true -> null
                false -> it.sequenceName
            }
            InsertionResponse(
                insertion = buildInsertion(sequenceName, it.position, it.insertedSymbols),
                count = it.count,
                insertedSymbols = it.insertedSymbols,
                position = it.position,
                sequenceName = sequenceName,
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
                insertion = buildInsertion(it.sequenceName, it.position, it.insertedSymbols),
                count = it.count,
                insertedSymbols = it.insertedSymbols,
                position = it.position,
                sequenceName = it.sequenceName,
            )
        }
    }

    fun getNewick(sequenceFilters: PhyloTreeSequenceFiltersRequest): Stream<PhyloSubtreeData> =
        try {
            siloClient.sendQuery(
                SiloQuery(
                    SiloAction.phyloSubtree(
                        sequenceFilters.phyloTreeField,
                    ),
                    siloFilterExpressionMapper.map(sequenceFilters),
                ),
            )
        } catch (exception: Exception) {
            if (exception.message?.contains(Regex("String value length \\(\\d+\\) exceeds the maximum allowed")) ==
                true
            ) {
                throw RuntimeException(
                    "The requested phylogeny is too large, please filter for a smaller subtree.",
                    exception,
                )
            } else {
                throw exception
            }
        }

    fun getGenomicSequence(
        sequenceFilters: CommonSequenceFilters,
        sequenceType: SequenceType,
        sequenceNames: List<String>,
        rawFastaHeaderTemplate: String,
        sequenceSymbolType: SequenceSymbolType,
    ): SequencesResponse {
        val fastaHeaderTemplate = fastaHeaderTemplateParser.parseTemplate(
            template = rawFastaHeaderTemplate,
            sequenceSymbolType = sequenceSymbolType,
        )
        val cleanedSequenceNames = sequenceNames
            .map { referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(it) ?: it }

        val sequenceData = siloClient.sendQuery(
            SiloQuery(
                SiloAction.genomicSequence(
                    type = sequenceType,
                    sequenceNames = mapSequenceNames(cleanedSequenceNames, sequenceType),
                    additionalFields = fastaHeaderTemplate.metadataFieldNames,
                    orderByFields = mapSequenceOrderByFields(
                        sequenceFilters.orderByFields,
                        sequenceType,
                    ),
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                ),
                siloFilterExpressionMapper.map(sequenceFilters),
            ),
        )

        return SequencesResponse(
            sequenceData = sequenceData,
            requestedSequenceNames = cleanedSequenceNames,
            fastaHeaderTemplate = fastaHeaderTemplate,
        )
    }

    private fun mapSequenceNames(
        sequenceNames: List<String>,
        sequenceType: SequenceType,
    ) = sequenceNames
        .map {
            when (sequenceType) {
                SequenceType.ALIGNED -> it
                SequenceType.UNALIGNED -> toUnalignedSequenceName(it)
            }
        }

    private fun mapSequenceOrderByFields(
        orderBySpec: OrderBySpec,
        sequenceType: SequenceType,
    ): OrderBySpec =
        when (orderBySpec) {
            is OrderBySpec.ByFields ->
                OrderBySpec.ByFields(
                    orderBySpec.fields
                        .map {
                            it.copy(
                                field =
                                    referenceGenomeSchema.getSequenceNameFromCaseInsensitiveName(it.field) ?: it.field,
                            )
                        }
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
                        },
                )
            is OrderBySpec.Random -> orderBySpec
        }

    fun getInfo(): InfoData = siloClient.callInfo()

    fun getLineageDefinition(column: String) = siloClient.getLineageDefinition(column)

    /**
     * SILO doesn't know the `mutation` field, instead we need the fields to assemble it.
     */
    private fun siloMutationFields(fields: List<MutationsField>): List<String> {
        if (fields.isEmpty()) {
            return emptyList()
        }
        val expanded = fields.toMutableSet()
        if (MutationsField.MUTATION in expanded) {
            expanded -= MutationsField.MUTATION
            expanded += MutationsField.MUTATION_FROM
            expanded += MutationsField.MUTATION_TO
            expanded += MutationsField.POSITION
            expanded += MutationsField.SEQUENCE_NAME
        }
        return expanded.map { it.value }
    }

    private fun mutationFromOrNull(
        sequenceFilters: MutationProportionsRequest,
        data: MutationData,
    ) = if (sequenceFilters.shouldResponseContainField(MutationsField.MUTATION_FROM)) {
        data.mutationFrom
    } else {
        null
    }

    private fun mutationToOrNull(
        sequenceFilters: MutationProportionsRequest,
        data: MutationData,
    ) = if (sequenceFilters.shouldResponseContainField(MutationsField.MUTATION_TO)) {
        data.mutationTo
    } else {
        null
    }

    private fun positionOrNull(
        sequenceFilters: MutationProportionsRequest,
        data: MutationData,
    ) = if (sequenceFilters.shouldResponseContainField(MutationsField.POSITION)) {
        data.position
    } else {
        null
    }

    private fun buildInsertion(
        sequenceName: String?,
        position: Int,
        insertedSymbols: String,
    ) = when (sequenceName) {
        null -> "ins_$position:$insertedSymbols"
        else -> "ins_$sequenceName:$position:$insertedSymbols"
    }
}

data class SequencesResponse(
    val sequenceData: Stream<SequenceData>,
    val requestedSequenceNames: List<String>,
    val fastaHeaderTemplate: FastaHeaderTemplate,
)
