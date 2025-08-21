package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.DatabaseConfig
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.LapisHeaders.LAPIS_DATA_VERSION
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_NEWICK_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA_VALUE
import org.genspectrum.lapis.controller.middleware.SequencesDataFormat
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SequenceSymbolType
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.AGGREGATED_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.ALL_ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.AggregatedOrderByFields
import org.genspectrum.lapis.openApi.AminoAcidFastaHeaderTemplateParam
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.AminoAcidSequencesOrderByFields
import org.genspectrum.lapis.openApi.DETAILS_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.DataFormat
import org.genspectrum.lapis.openApi.DetailsFields
import org.genspectrum.lapis.openApi.DetailsOrderByFields
import org.genspectrum.lapis.openApi.FieldsToAggregateBy
import org.genspectrum.lapis.openApi.GENES_DESCRIPTION
import org.genspectrum.lapis.openApi.GENE_SCHEMA
import org.genspectrum.lapis.openApi.Gene
import org.genspectrum.lapis.openApi.INSERTIONS_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.InsertionsOrderByFields
import org.genspectrum.lapis.openApi.LapisAggregatedResponse
import org.genspectrum.lapis.openApi.LapisAlignedAminoAcidSequenceResponse
import org.genspectrum.lapis.openApi.LapisAllAminoAcidSequencesResponse
import org.genspectrum.lapis.openApi.LapisAminoAcidInsertionsResponse
import org.genspectrum.lapis.openApi.LapisAminoAcidMutationsResponse
import org.genspectrum.lapis.openApi.LapisDetailsResponse
import org.genspectrum.lapis.openApi.LapisMostRecentCommonAncestorResponse
import org.genspectrum.lapis.openApi.LapisNucleotideInsertionsResponse
import org.genspectrum.lapis.openApi.LapisNucleotideMutationsResponse
import org.genspectrum.lapis.openApi.LapisPhyloSubtreeResponse
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.MOST_RECENT_COMMON_ANCESTOR_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.MutationsFields
import org.genspectrum.lapis.openApi.MutationsOrderByFields
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PHYLO_SUBTREE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.PhyloTreeField
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
import org.genspectrum.lapis.openApi.REQUEST_SCHEMA_WITH_MIN_PROPORTION
import org.genspectrum.lapis.openApi.SequencesDataFormatParam
import org.genspectrum.lapis.openApi.StringResponseOperation
import org.genspectrum.lapis.openApi.TreeDataFormatParam
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.CaseInsensitiveFieldConverter
import org.genspectrum.lapis.request.DEFAULT_MIN_PROPORTION
import org.genspectrum.lapis.request.GetRequestSequenceFilters
import org.genspectrum.lapis.request.MRCASequenceFiltersRequest
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.PhyloTreeSequenceFiltersRequest
import org.genspectrum.lapis.request.SPECIAL_REQUEST_PROPERTIES
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.request.SequenceFiltersRequestWithGenes
import org.genspectrum.lapis.request.validatePhyloTreeField
import org.genspectrum.lapis.response.AggregatedCollection
import org.genspectrum.lapis.response.Delimiter.COMMA
import org.genspectrum.lapis.response.Delimiter.TAB
import org.genspectrum.lapis.response.DetailsCollection
import org.genspectrum.lapis.response.InsertionsCollection
import org.genspectrum.lapis.response.LapisResponseStreamer
import org.genspectrum.lapis.response.MostRecentCommonAncestorCollection
import org.genspectrum.lapis.response.MutationsCollection
import org.genspectrum.lapis.response.ResponseFormat
import org.genspectrum.lapis.response.SequencesStreamer
import org.genspectrum.lapis.silo.DataVersion
import org.genspectrum.lapis.silo.SequenceType
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sample")
class LapisController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val caseInsensitiveFieldConverter: CaseInsensitiveFieldConverter,
    private val sequencesStreamer: SequencesStreamer,
    private val lapisResponseStreamer: LapisResponseStreamer,
    private val databaseConfig: DatabaseConfig,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
    private val dataVersion: DataVersion,
) {
    @GetMapping(AGGREGATED_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAggregatedResponse
    fun aggregated(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @FieldsToAggregateBy
        @RequestParam
        fields: List<String>?,
        @AggregatedOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(AGGREGATED_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "getAggregatedAsCsv",
    )
    fun getAggregatedAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @FieldsToAggregateBy
        @RequestParam
        fields: List<String>?,
        @AggregatedOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(AGGREGATED_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "getAggregatedAsTsv",
    )
    fun getAggregatedAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @FieldsToAggregateBy
        @RequestParam
        fields: List<String>?,
        @AggregatedOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AGGREGATED_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAggregatedResponse
    @Operation(
        operationId = "postAggregated",
    )
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        AGGREGATED_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "postAggregatedAsCsv",
    )
    fun postAggregatedAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AGGREGATED_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "postAggregatedAsTsv",
    )
    fun postAggregatedAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAggregatedCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getAggregatedCollection(request: SequenceFiltersRequestWithFields): AggregatedCollection =
        AggregatedCollection(
            records = siloQueryModel.getAggregated(request),
            fields = request.fields.map { it.fieldName },
        )

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideMutationsResponse
    fun getNucleotideMutations(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        response: HttpServletResponse,
    ) {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideMutationsAsCsv",
    )
    fun getNucleotideMutationsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideMutationsAsTsv",
    )
    fun getNucleotideMutationsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        NUCLEOTIDE_MUTATIONS_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisNucleotideMutationsResponse
    @Operation(
        operationId = "postNucleotideMutations",
    )
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        NUCLEOTIDE_MUTATIONS_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideMutationsAsCsv",
    )
    fun postNucleotideMutationsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        NUCLEOTIDE_MUTATIONS_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideMutationsAsTsv",
    )
    fun postNucleotideMutationsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getNucleotideMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getNucleotideMutationsCollection(request: MutationProportionsRequest) =
        MutationsCollection(
            records = siloQueryModel.computeNucleotideMutationProportions(request),
            fields = when (request.fields.isEmpty()) {
                true -> MutationsField.entries
                false -> request.fields
            },
        )

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidMutationsResponse
    fun getAminoAcidMutations(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        response: HttpServletResponse,
    ) {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidMutationsAsCsv",
    )
    fun getAminoAcidMutationsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidMutationsAsTsv",
    )
    fun getAminoAcidMutationsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam(defaultValue = "$DEFAULT_MIN_PROPORTION") minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @MutationsFields
        @RequestParam
        fields: List<String>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters = sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations = nucleotideMutations ?: emptyList(),
            aminoAcidMutations = aminoAcidMutations ?: emptyList(),
            nucleotideInsertions = nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions = aminoAcidInsertions ?: emptyList(),
            fields = fields?.map { MutationsField.fromString(it) } ?: emptyList(),
            minProportion = minProportion,
            orderByFields = orderBy ?: emptyList(),
            limit = limit,
            offset = offset,
        )

        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AMINO_ACID_MUTATIONS_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAminoAcidMutationsResponse
    @Operation(
        operationId = "postAminoAcidMutations",
    )
    fun postAminoAcidMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        AMINO_ACID_MUTATIONS_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidMutationsAsCsv",
    )
    fun postAminoAcidMutationsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AMINO_ACID_MUTATIONS_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidMutationsAsCsv",
    )
    fun postAminoAcidMutationsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = mutationProportionsRequest,
            getData = ::getAminoAcidMutationsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getAminoAcidMutationsCollection(request: MutationProportionsRequest) =
        MutationsCollection(
            records = siloQueryModel.computeAminoAcidMutationProportions(request),
            fields = when (request.fields.isEmpty()) {
                true -> MutationsField.entries
                false -> request.fields
            },
        )

    @GetMapping(MOST_RECENT_COMMON_ANCESTOR_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisMostRecentCommonAncestorResponse
    fun getMostRecentCommonAncestorAsJson(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @PhyloTreeField
        @RequestParam
        phyloTreeField: String,
        @Parameter(
            description = PRINT_NODES_NOT_IN_TREE_FIELD_DESCRIPTION,
        )
        @RequestParam(required = false, defaultValue = "false")
        printNodesNotInTree: Boolean,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        response: HttpServletResponse,
    ) {
        val request = MRCASequenceFiltersRequest(
            sequenceFilters?.filterKeys { !SPECIAL_REQUEST_PROPERTIES.contains(it) }
                ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            phyloTreeField = validatePhyloTreeField(
                phyloTreeField,
                caseInsensitiveFieldConverter,
                databaseConfig,
            ).fieldName,
            printNodesNotInTree = printNodesNotInTree,
        )
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(MOST_RECENT_COMMON_ANCESTOR_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        operationId = "getMostRecentCommonAncestorAsCsv",
    )
    fun getMostRecentCommonAncestorAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @PhyloTreeField
        @RequestParam
        phyloTreeField: String,
        @Parameter(
            description = PRINT_NODES_NOT_IN_TREE_FIELD_DESCRIPTION,
        )
        @RequestParam(required = false, defaultValue = "false")
        printNodesNotInTree: Boolean,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = MRCASequenceFiltersRequest(
            sequenceFilters?.filterKeys { !SPECIAL_REQUEST_PROPERTIES.contains(it) }
                ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            phyloTreeField =
                validatePhyloTreeField(phyloTreeField, caseInsensitiveFieldConverter, databaseConfig).fieldName,
            printNodesNotInTree = printNodesNotInTree,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(MOST_RECENT_COMMON_ANCESTOR_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        operationId = "getMostRecentCommonAncestorAsTsv",
    )
    fun getMostRecentCommonAncestorAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @PhyloTreeField
        @RequestParam
        phyloTreeField: String,
        @Parameter(
            description = PRINT_NODES_NOT_IN_TREE_FIELD_DESCRIPTION,
        )
        @RequestParam(required = false, defaultValue = "false")
        printNodesNotInTree: Boolean,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = MRCASequenceFiltersRequest(
            sequenceFilters?.filterKeys { !SPECIAL_REQUEST_PROPERTIES.contains(it) }
                ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            phyloTreeField = validatePhyloTreeField(
                phyloTreeField,
                caseInsensitiveFieldConverter,
                databaseConfig,
            ).fieldName,
            printNodesNotInTree = printNodesNotInTree,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        MOST_RECENT_COMMON_ANCESTOR_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisMostRecentCommonAncestorResponse
    @Operation(
        operationId = "postMostRecentCommonAncestor",
    )
    fun postMostRecentCommonAncestorAsJson(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MOST_RECENT_COMMON_ANCESTOR_REQUEST_SCHEMA"))
        @RequestBody
        request: MRCASequenceFiltersRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        MOST_RECENT_COMMON_ANCESTOR_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = MOST_RECENT_COMMON_ANCESTOR_ENDPOINT_DESCRIPTION,
        operationId = "postMostRecentCommonAncestorAsCsv",
    )
    fun postMostRecentCommonAncestorAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MOST_RECENT_COMMON_ANCESTOR_REQUEST_SCHEMA"))
        @RequestBody
        request: MRCASequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        MOST_RECENT_COMMON_ANCESTOR_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = MOST_RECENT_COMMON_ANCESTOR_ENDPOINT_DESCRIPTION,
        operationId = "postMostRecentCommonAncestorAsTsv",
    )
    fun postMostRecentCommonAncestorAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$MOST_RECENT_COMMON_ANCESTOR_REQUEST_SCHEMA"))
        @RequestBody
        request: MRCASequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getMostRecentCommonAncestorCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getMostRecentCommonAncestorCollection(
        request: MRCASequenceFiltersRequest,
    ): MostRecentCommonAncestorCollection {
        validatePhyloTreeField(request.phyloTreeField, caseInsensitiveFieldConverter, databaseConfig)
        return MostRecentCommonAncestorCollection(
            records = siloQueryModel.getMostRecentCommonAncestor(request),
            fields = listOf("mrcaNode", "missingNodeCount", "missingFromTree"),
        )
    }

    @GetMapping(PHYLO_SUBTREE_ROUTE, produces = [TEXT_NEWICK_VALUE])
    @LapisPhyloSubtreeResponse
    fun getPhyloSubtreeAsNewick(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @PhyloTreeField
        @RequestParam
        phyloTreeField: String,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @TreeDataFormatParam
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        response: HttpServletResponse,
    ): String {
        val request = PhyloTreeSequenceFiltersRequest(
            sequenceFilters?.filterKeys { !SPECIAL_REQUEST_PROPERTIES.contains(it) }
                ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            phyloTreeField = validatePhyloTreeField(
                phyloTreeField,
                caseInsensitiveFieldConverter,
                databaseConfig,
            ).fieldName,
        )

        return getNewickSubtree(request, response)
    }

    @PostMapping(
        PHYLO_SUBTREE_ROUTE,
        produces = [TEXT_NEWICK_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisPhyloSubtreeResponse
    @Operation(
        operationId = "postPhyloSubtreeAsNewick",
    )
    fun postPhyloSubtreeAsNewick(
        @Parameter(schema = Schema(ref = "#/components/schemas/$PHYLO_SUBTREE_REQUEST_SCHEMA"))
        @RequestBody
        request: PhyloTreeSequenceFiltersRequest,
        response: HttpServletResponse,
    ): String = getNewickSubtree(request, response)

    private fun getNewickSubtree(
        request: PhyloTreeSequenceFiltersRequest,
        response: HttpServletResponse,
    ): String {
        val treeResponse = siloQueryModel.getNewick(sequenceFilters = request)
        response.setHeader(LAPIS_DATA_VERSION, dataVersion.dataVersion)
        return treeResponse.toList().first().subtreeNewick
    }

    @GetMapping(DETAILS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    fun getDetailsAsJson(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @DetailsFields
        @RequestParam
        fields: List<String>?,
        @DetailsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(DETAILS_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        operationId = "getDetailsAsCsv",
    )
    fun getDetailsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @DetailsFields
        @RequestParam
        fields: List<String>?,
        @DetailsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(DETAILS_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "getDetailsAsTsv",
    )
    fun getDetailsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @DetailsFields
        @RequestParam
        fields: List<String>?,
        @DetailsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields?.map { caseInsensitiveFieldConverter.convert(it) } ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        DETAILS_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisDetailsResponse
    @Operation(
        operationId = "postDetails",
    )
    fun postDetails(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        DETAILS_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetailsAsCsv",
    )
    fun postDetailsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        DETAILS_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetailsAsTsv",
    )
    fun postDetailsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getDetailsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getDetailsCollection(request: SequenceFiltersRequestWithFields): DetailsCollection {
        val fields = request.fields.map { it.fieldName }

        return DetailsCollection(
            records = siloQueryModel.getDetails(request),
            fields = when (fields.isEmpty()) {
                true -> databaseConfig.schema.metadata.map { it.name }
                false -> fields
            },
        )
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideInsertionsResponse
    fun getNucleotideInsertions(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideInsertionsAsCsv",
    )
    fun getNucleotideInsertionsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideInsertionsAsTsv",
    )
    fun getNucleotideInsertionsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        NUCLEOTIDE_INSERTIONS_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisNucleotideInsertionsResponse
    @Operation(
        operationId = "postNucleotideInsertions",
    )
    fun postNucleotideInsertions(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        NUCLEOTIDE_INSERTIONS_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideInsertionsAsCsv",
    )
    fun postNucleotideInsertionsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        NUCLEOTIDE_INSERTIONS_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideInsertionsAsTsv",
    )
    fun postNucleotideInsertionsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getNucleotideInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getNucleotideInsertionsCollection(request: SequenceFiltersRequest) =
        InsertionsCollection(
            records = siloQueryModel.getNucleotideInsertions(request),
        )

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidInsertionsResponse
    fun getAminoAcidInsertions(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_CSV_VALUE])
    @StringResponseOperation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidInsertionsAsCsv",
    )
    fun getAminoAcidInsertionsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_TSV_VALUE])
    @StringResponseOperation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidInsertionsAsTsv",
    )
    fun getAminoAcidInsertionsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @InsertionsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AMINO_ACID_INSERTIONS_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAminoAcidInsertionsResponse
    @Operation(
        operationId = "postAminoAcidInsertions",
    )
    fun postAminoAcidInsertions(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        AMINO_ACID_INSERTIONS_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidInsertionsAsCsv",
    )
    fun postAminoAcidInsertionsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = COMMA,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    @PostMapping(
        AMINO_ACID_INSERTIONS_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidInsertionsAsTsv",
    )
    fun postAminoAcidInsertionsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = ::getAminoAcidInsertionsCollection,
            response = response,
            responseFormat = ResponseFormat.Csv(
                delimiter = TAB,
                acceptHeader = httpHeaders.accept,
            ),
        )
    }

    private fun getAminoAcidInsertionsCollection(request: SequenceFiltersRequest) =
        InsertionsCollection(
            records = siloQueryModel.getAminoAcidInsertions(request),
        )

    @GetMapping(
        ALIGNED_AMINO_ACID_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisAllAminoAcidSequencesResponse
    fun getAllAlignedAminoAcidSequences(
        @RequestParam
        @Parameter(
            array = ArraySchema(schema = Schema(ref = "#/components/schemas/$GENE_SCHEMA")),
            description = GENES_DESCRIPTION,
        )
        genes: List<String>?,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @AminoAcidSequencesOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @SequencesDataFormatParam
        @RequestParam
        dataFormat: String? = null,
        @AminoAcidFastaHeaderTemplateParam
        @RequestParam
        fastaHeaderTemplate: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        val sequencesDataFormat = SequencesDataFormat.fromAcceptHeaders(httpHeaders.accept)

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = genes ?: referenceGenomeSchema.getGeneNames(),
            rawFastaHeaderTemplate = getFastaHeaderTemplate(
                requestedTemplate = fastaHeaderTemplate,
                defaultTemplate = "{${databaseConfig.schema.primaryKey}}|{.gene}",
                sequencesDataFormat = sequencesDataFormat,
            ),
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )
            .also {
                sequencesStreamer.stream(
                    sequencesResponse = it,
                    response = response,
                    sequencesDataFormat = sequencesDataFormat,
                )
            }
    }

    @PostMapping(
        ALIGNED_AMINO_ACID_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAllAminoAcidSequencesResponse
    fun postAllAlignedAminoAcidSequences(
        @Parameter(schema = Schema(ref = "#/components/schemas/$ALL_ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithGenes,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        val sequencesDataFormat = SequencesDataFormat.fromAcceptHeaders(httpHeaders.accept)

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = request.genes,
            rawFastaHeaderTemplate = getFastaHeaderTemplate(
                requestedTemplate = request.fastaHeaderTemplate,
                defaultTemplate = "{${databaseConfig.schema.primaryKey}}|{.gene}",
                sequencesDataFormat = sequencesDataFormat,
            ),
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )
            .also {
                sequencesStreamer.stream(
                    sequencesResponse = it,
                    response = response,
                    sequencesDataFormat = sequencesDataFormat,
                )
            }
    }

    @GetMapping(
        "$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/{gene}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisAlignedAminoAcidSequenceResponse
    fun getAlignedAminoAcidSequence(
        @PathVariable(name = "gene", required = true)
        @Gene
        gene: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @AminoAcidSequencesOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @SequencesDataFormatParam
        @RequestParam
        dataFormat: String? = null,
        @AminoAcidFastaHeaderTemplateParam
        @RequestParam
        fastaHeaderTemplate: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        val sequencesDataFormat = SequencesDataFormat.fromAcceptHeaders(httpHeaders.accept)

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = listOf(gene),
            rawFastaHeaderTemplate = getFastaHeaderTemplate(
                requestedTemplate = fastaHeaderTemplate,
                defaultTemplate = "{${databaseConfig.schema.primaryKey}}",
                sequencesDataFormat = sequencesDataFormat,
            ),
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )
            .also {
                sequencesStreamer.stream(
                    sequencesResponse = it,
                    response = response,
                    sequencesDataFormat = sequencesDataFormat,
                )
            }
    }

    @PostMapping(
        "$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/{gene}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAlignedAminoAcidSequenceResponse
    fun postAlignedAminoAcidSequence(
        @PathVariable(name = "gene", required = true)
        @Gene
        gene: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        val sequencesDataFormat = SequencesDataFormat.fromAcceptHeaders(httpHeaders.accept)

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = listOf(gene),
            rawFastaHeaderTemplate = getFastaHeaderTemplate(
                requestedTemplate = request.fastaHeaderTemplate,
                defaultTemplate = "{${databaseConfig.schema.primaryKey}}",
                sequencesDataFormat = sequencesDataFormat,
            ),
            sequenceSymbolType = SequenceSymbolType.AMINO_ACID,
        )
            .also {
                sequencesStreamer.stream(
                    sequencesResponse = it,
                    response = response,
                    sequencesDataFormat = sequencesDataFormat,
                )
            }
    }
}
