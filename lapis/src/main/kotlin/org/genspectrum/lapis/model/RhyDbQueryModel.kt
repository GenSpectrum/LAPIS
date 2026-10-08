package org.genspectrum.lapis.model

import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.request.AggregatedFiltersRequest
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.ComputedField
import org.genspectrum.lapis.request.DetailsFiltersRequest
import org.genspectrum.lapis.request.MRCASequenceFiltersRequest
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.OrderByField
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
import org.genspectrum.lapis.rhydb.RhyDbAction
import org.genspectrum.lapis.rhydb.RhyDbClient
import org.genspectrum.lapis.rhydb.RhyDbQuery
import org.genspectrum.lapis.rhydb.SequenceType
import org.genspectrum.lapis.util.toUnalignedSequenceName
import org.springframework.stereotype.Component
import java.util.stream.Stream

private const val INSERTION_FIELD = "insertion"
private val INSERTION_COMPONENT_FIELDS = listOf("sequenceName", "position", "insertedSymbols")

@Component
class RhyDbQueryModel(
    private val rhyDbClient: RhyDbClient,
    private val rhyDbFilterExpressionMapper: RhyDbFilterExpressionMapper,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    private val fastaHeaderTemplateParser: FastaHeaderTemplateParser,
    databaseConfig: DatabaseConfig,
) {
    private val allMetadataFields = databaseConfig.schema.metadata.map { it.name }

    fun getAggregated(sequenceFilters: AggregatedFiltersRequest) =
        rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.aggregated(
                    groupByFields = sequenceFilters.fields.filterIsInstance<PlainField>().map { it.fieldName },
                    orderByFields = sequenceFilters.orderByFields,
                    limit = sequenceFilters.limit,
                    offset = sequenceFilters.offset,
                    sequencePositionFields = sequenceFilters.fields.filterIsInstance<SequencePositionField>(),
                    computedFields = sequenceFilters.fields.filterIsInstance<ComputedField>(),
                ),
                rhyDbFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun computeNucleotideMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val assembleMutation = { data: MutationData ->
            val core = "${data.mutationFrom}${data.position}${data.mutationTo}"
            if (referenceGenomeSchema.isSingleSegmented()) core else "${data.sequenceName}:$core"
        }

        return queryMutationData(sequenceFilters, RhyDbAction.Companion::mutations).map {
            MutationResponse(
                mutation = sequenceFilters.ifRequested(MutationsField.MUTATION, assembleMutation(it)),
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
                mutationFrom = sequenceFilters.ifRequested(MutationsField.MUTATION_FROM, it.mutationFrom),
                mutationTo = sequenceFilters.ifRequested(MutationsField.MUTATION_TO, it.mutationTo),
                position = sequenceFilters.ifRequested(MutationsField.POSITION, it.position),
            )
        }
    }

    fun computeAminoAcidMutationProportions(sequenceFilters: MutationProportionsRequest): Stream<MutationResponse> {
        val assembleMutation = { data: MutationData ->
            "${data.sequenceName}:${data.mutationFrom}${data.position}${data.mutationTo}"
        }

        return queryMutationData(sequenceFilters, RhyDbAction.Companion::aminoAcidMutations).map {
            MutationResponse(
                mutation = sequenceFilters.ifRequested(MutationsField.MUTATION, assembleMutation(it)),
                count = it.count,
                coverage = it.coverage,
                proportion = it.proportion,
                sequenceName = if (!sequenceFilters.shouldResponseContainField(MutationsField.SEQUENCE_NAME)) {
                    null
                } else {
                    ExplicitlyNullable(it.sequenceName)
                },
                mutationFrom = sequenceFilters.ifRequested(MutationsField.MUTATION_FROM, it.mutationFrom),
                mutationTo = sequenceFilters.ifRequested(MutationsField.MUTATION_TO, it.mutationTo),
                position = sequenceFilters.ifRequested(MutationsField.POSITION, it.position),
            )
        }
    }

    fun getDetails(sequenceFilters: DetailsFiltersRequest) =
        rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.details(
                    sequenceFilters.fields.map { it.fieldName }.ifEmpty { allMetadataFields },
                    sequenceFilters.orderByFields,
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                rhyDbFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun getMostRecentCommonAncestor(sequenceFilters: MRCASequenceFiltersRequest) =
        rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.mostRecentCommonAncestor(
                    sequenceFilters.phyloTreeField,
                    sequenceFilters.printNodesNotInTree,
                ),
                rhyDbFilterExpressionMapper.map(sequenceFilters),
            ),
        )

    fun getNucleotideInsertions(sequenceFilters: SequenceFiltersRequest): Stream<InsertionResponse> {
        val data = rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.nucleotideInsertions(
                    expandInsertionOrderBy(sequenceFilters.orderByFields),
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                rhyDbFilterExpressionMapper.map(sequenceFilters),
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
        val data = rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.aminoAcidInsertions(
                    expandInsertionOrderBy(sequenceFilters.orderByFields),
                    sequenceFilters.limit,
                    sequenceFilters.offset,
                ),
                rhyDbFilterExpressionMapper.map(sequenceFilters),
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
            rhyDbClient.sendQuery(
                RhyDbQuery(
                    RhyDbAction.phyloSubtree(
                        sequenceFilters.phyloTreeField,
                    ),
                    rhyDbFilterExpressionMapper.map(sequenceFilters),
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

        val sequenceData = rhyDbClient.sendQuery(
            RhyDbQuery(
                RhyDbAction.genomicSequence(
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
                rhyDbFilterExpressionMapper.map(sequenceFilters),
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

    fun getInfo(): InfoData = rhyDbClient.callInfo()

    fun getLineageDefinition(column: String) = rhyDbClient.getLineageDefinition(column)

    private fun queryMutationData(
        sequenceFilters: MutationProportionsRequest,
        actionConstructor: (
            minProportion: Double?,
            orderByFields: OrderBySpec,
            limit: Int?,
            offset: Int?,
            fields: List<String>,
        ) -> RhyDbAction<MutationData>,
    ): Stream<MutationData> {
        val fields = rhyDbMutationFields(sequenceFilters)

        val action = actionConstructor(
            sequenceFilters.minProportion,
            expandMutationOrderBy(sequenceFilters.orderByFields),
            sequenceFilters.limit,
            sequenceFilters.offset,
            fields,
        )
        return rhyDbClient.sendQuery(RhyDbQuery(action, rhyDbFilterExpressionMapper.map(sequenceFilters)))
    }

    /**
     * Since SILO can't order by the assembled `mutation` field, replace any `mutation` order-by entry with its
     * component fields (in `sequenceName`, `mutationFrom`, `position`, `mutationTo` order), keeping the direction.
     */
    private fun expandMutationOrderBy(orderByFields: OrderBySpec): OrderBySpec =
        when (orderByFields) {
            is OrderBySpec.ByFields -> OrderBySpec.ByFields(
                orderByFields.fields.flatMap { field ->
                    when (field.field) {
                        MutationsField.MUTATION.value -> listOf(
                            MutationsField.SEQUENCE_NAME,
                            MutationsField.MUTATION_FROM,
                            MutationsField.POSITION,
                            MutationsField.MUTATION_TO,
                        ).map { OrderByField(it.value, field.order) }

                        else -> listOf(field)
                    }
                },
            )

            is OrderBySpec.Random -> orderByFields
        }

    /**
     * Since SILO can't order by the assembled `insertion` field, replace any `insertion` order-by entry with its
     * component fields (in `sequenceName`, `position`, `insertedSymbols` order), keeping the direction.
     */
    private fun expandInsertionOrderBy(orderByFields: OrderBySpec): OrderBySpec =
        when (orderByFields) {
            is OrderBySpec.ByFields -> OrderBySpec.ByFields(
                orderByFields.fields.flatMap { field ->
                    when (field.field) {
                        INSERTION_FIELD -> INSERTION_COMPONENT_FIELDS.map { OrderByField(it, field.order) }

                        else -> listOf(field)
                    }
                },
            )

            is OrderBySpec.Random -> orderByFields
        }

    private fun rhyDbMutationFields(request: MutationProportionsRequest): List<String> {
        val fields = request.fields
        if (fields.isEmpty()) {
            return emptyList()
        }

        val hasOrderByMutation = request.orderByFields is OrderBySpec.ByFields &&
            request.orderByFields.fields.any { it.field == MutationsField.MUTATION.value }

        val expanded = fields.toMutableSet()
        if (hasOrderByMutation || MutationsField.MUTATION in expanded) {
            expanded -= MutationsField.MUTATION
            expanded += MutationsField.MUTATION_FROM
            expanded += MutationsField.MUTATION_TO
            expanded += MutationsField.POSITION
            expanded += MutationsField.SEQUENCE_NAME
        }
        return expanded.map { it.value }
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
