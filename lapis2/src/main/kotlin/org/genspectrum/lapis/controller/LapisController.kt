package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.controller.Delimiter.COMMA
import org.genspectrum.lapis.controller.Delimiter.TAB
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.AGGREGATED_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.AggregatedOrderByFields
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.AminoAcidSequencesOrderByFields
import org.genspectrum.lapis.openApi.DETAILS_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.DataFormat
import org.genspectrum.lapis.openApi.DetailsFields
import org.genspectrum.lapis.openApi.DetailsOrderByFields
import org.genspectrum.lapis.openApi.FieldsToAggregateBy
import org.genspectrum.lapis.openApi.INSERTIONS_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.InsertionsOrderByFields
import org.genspectrum.lapis.openApi.LapisAggregatedResponse
import org.genspectrum.lapis.openApi.LapisAlignedAminoAcidSequenceResponse
import org.genspectrum.lapis.openApi.LapisAminoAcidInsertionsResponse
import org.genspectrum.lapis.openApi.LapisAminoAcidMutationsResponse
import org.genspectrum.lapis.openApi.LapisDetailsResponse
import org.genspectrum.lapis.openApi.LapisNucleotideInsertionsResponse
import org.genspectrum.lapis.openApi.LapisNucleotideMutationsResponse
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.MutationsOrderByFields
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
import org.genspectrum.lapis.openApi.REQUEST_SCHEMA_WITH_MIN_PROPORTION
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.CommonSequenceFilters
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.AminoAcidInsertionResponse
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.NucleotideInsertionResponse
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.genspectrum.lapis.silo.SequenceType
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val AGGREGATED_ROUTE = "/aggregated"
const val DETAILS_ROUTE = "/details"
const val NUCLEOTIDE_MUTATIONS_ROUTE = "/nucleotideMutations"
const val AMINO_ACID_MUTATIONS_ROUTE = "/aminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_ROUTE = "/nucleotideInsertions"
const val AMINO_ACID_INSERTIONS_ROUTE = "/aminoAcidInsertions"
const val ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE = "/alignedNucleotideSequences"
const val ALIGNED_AMINO_ACID_SEQUENCES_ROUTE = "/alignedAminoAcidSequences"

@RestController
class LapisController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val csvWriter: CsvWriter,
) {
    @GetMapping(AGGREGATED_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAggregatedResponse
    fun aggregated(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): LapisResponse<List<AggregationData>> {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        return LapisResponse(siloQueryModel.getAggregated(request))
    }

    @GetMapping(AGGREGATED_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "getAggregatedAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAggregatedAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        return getResponseAsCsv(request, COMMA, siloQueryModel::getAggregated)
    }

    @GetMapping(AGGREGATED_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "getAggregatedAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAggregatedAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        return getResponseAsCsv(request, TAB, siloQueryModel::getAggregated)
    }

    @PostMapping(AGGREGATED_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAggregatedResponse
    @Operation(
        operationId = "postAggregated",
    )
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): LapisResponse<List<AggregationData>> {
        requestContext.filter = request

        return LapisResponse(siloQueryModel.getAggregated(request))
    }

    @PostMapping(AGGREGATED_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "postAggregatedAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAggregatedAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        return getResponseAsCsv(request, COMMA, siloQueryModel::getAggregated)
    }

    @PostMapping(AGGREGATED_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPOINT_DESCRIPTION,
        operationId = "postAggregatedAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAggregatedAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        return getResponseAsCsv(request, TAB, siloQueryModel::getAggregated)
    }

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideMutationsResponse
    fun getNucleotideMutations(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @MutationsOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        request: HttpServletRequest,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
    ): LapisResponse<List<NucleotideMutationResponse>> {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = mutationProportionsRequest

        val result = siloQueryModel.computeNucleotideMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideMutationsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getNucleotideMutationsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
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
    ): String {
        val request = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request

        return getResponseAsCsv(request, COMMA, siloQueryModel::computeNucleotideMutationProportions)
    }

    @GetMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideMutationsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getNucleotideMutationsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
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
    ): String {
        val request = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request

        return getResponseAsCsv(request, TAB, siloQueryModel::computeNucleotideMutationProportions)
    }

    @PostMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideMutationsResponse
    @Operation(
        operationId = "postNucleotideMutations",
    )
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): LapisResponse<List<NucleotideMutationResponse>> {
        requestContext.filter = mutationProportionsRequest

        val result = siloQueryModel.computeNucleotideMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @PostMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideMutationsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postNucleotideMutationsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): String {
        return getResponseAsCsv(mutationProportionsRequest, COMMA, siloQueryModel::computeNucleotideMutationProportions)
    }

    @PostMapping(NUCLEOTIDE_MUTATIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideMutationsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postNucleotideMutationsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): String {
        return getResponseAsCsv(mutationProportionsRequest, TAB, siloQueryModel::computeNucleotideMutationProportions)
    }

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidMutationsResponse
    fun getAminoAcidMutations(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
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
    ): LapisResponse<List<AminoAcidMutationResponse>> {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = mutationProportionsRequest

        val result = siloQueryModel.computeAminoAcidMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidMutationsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAminoAcidMutationsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
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
    ): String {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = mutationProportionsRequest

        return getResponseAsCsv(mutationProportionsRequest, COMMA, siloQueryModel::computeAminoAcidMutationProportions)
    }

    @GetMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidMutationsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAminoAcidMutationsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @NucleotideMutations
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @AminoAcidMutations
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
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
    ): String {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = mutationProportionsRequest

        return getResponseAsCsv(
            mutationProportionsRequest,
            TAB,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @PostMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidMutationsResponse
    @Operation(
        operationId = "postAminoAcidMutations",
    )
    fun postAminoAcidMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): LapisResponse<List<AminoAcidMutationResponse>> {
        requestContext.filter = mutationProportionsRequest

        val result = siloQueryModel.computeAminoAcidMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @PostMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidMutationsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAminoAcidMutationsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): String {
        requestContext.filter = mutationProportionsRequest

        return getResponseAsCsv(
            mutationProportionsRequest,
            COMMA,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @PostMapping(AMINO_ACID_MUTATIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidMutationsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAminoAcidMutationsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        mutationProportionsRequest: MutationProportionsRequest,
    ): String {
        requestContext.filter = mutationProportionsRequest

        return getResponseAsCsv(
            mutationProportionsRequest,
            TAB,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @GetMapping(DETAILS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    fun getDetailsAsJson(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): LapisResponse<List<DetailsData>> {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request

        return LapisResponse(siloQueryModel.getDetails(request))
    }

    @GetMapping(DETAILS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        operationId = "getDetailsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getDetailsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request
        return getResponseAsCsv(request, COMMA, siloQueryModel::getDetails)
    }

    @GetMapping(DETAILS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "getDetailsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getDetailsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        return getResponseAsCsv(request, TAB, siloQueryModel::getDetails)
    }

    @PostMapping(DETAILS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    @Operation(
        operationId = "postDetails",
    )
    fun postDetails(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): LapisResponse<List<DetailsData>> {
        requestContext.filter = request

        return LapisResponse(siloQueryModel.getDetails(request))
    }

    @PostMapping(DETAILS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetailsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postDetailsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        return getResponseAsCsv(request, COMMA, siloQueryModel::getDetails)
    }

    @PostMapping(DETAILS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetailsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postDetailsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        return getResponseAsCsv(request, TAB, siloQueryModel::getDetails)
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideInsertionsResponse
    fun getNucleotideInsertions(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): LapisResponse<List<NucleotideInsertionResponse>> {
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

        val result = siloQueryModel.getNucleotideInsertions(request)
        return LapisResponse(result)
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideInsertionsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getNucleotideInsertionsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return getResponseAsCsv(request, COMMA, siloQueryModel::getNucleotideInsertions)
    }

    @GetMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideInsertionsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getNucleotideInsertionsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return getResponseAsCsv(request, TAB, siloQueryModel::getNucleotideInsertions)
    }

    @PostMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideInsertionsResponse
    @Operation(
        operationId = "postNucleotideInsertions",
    )
    fun postNucleotideInsertions(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): LapisResponse<List<NucleotideInsertionResponse>> {
        requestContext.filter = request

        val result = siloQueryModel.getNucleotideInsertions(request)
        return LapisResponse(result)
    }

    @PostMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideInsertionsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postNucleotideInsertionsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return getResponseAsCsv(request, COMMA, siloQueryModel::getNucleotideInsertions)
    }

    @PostMapping(NUCLEOTIDE_INSERTIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideInsertionsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postNucleotideInsertionsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return getResponseAsCsv(request, TAB, siloQueryModel::getNucleotideInsertions)
    }

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidInsertionsResponse
    fun getAminoAcidInsertions(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): LapisResponse<List<AminoAcidInsertionResponse>> {
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

        val result = siloQueryModel.getAminoAcidInsertions(request)
        return LapisResponse(result)
    }

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidInsertionsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAminoAcidInsertionsAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return getResponseAsCsv(request, COMMA, siloQueryModel::getAminoAcidInsertions)
    }

    @GetMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidInsertionsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getAminoAcidInsertionsAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return getResponseAsCsv(request, TAB, siloQueryModel::getAminoAcidInsertions)
    }

    @PostMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidInsertionsResponse
    @Operation(
        operationId = "postAminoAcidInsertions",
    )
    fun postAminoAcidInsertions(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): LapisResponse<List<AminoAcidInsertionResponse>> {
        requestContext.filter = request

        val result = siloQueryModel.getAminoAcidInsertions(request)
        return LapisResponse(result)
    }

    @PostMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidInsertionsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAminoAcidInsertionsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return getResponseAsCsv(request, COMMA, siloQueryModel::getAminoAcidInsertions)
    }

    @PostMapping(AMINO_ACID_INSERTIONS_ROUTE, produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidInsertionsAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAminoAcidInsertionsAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$INSERTIONS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return getResponseAsCsv(request, TAB, siloQueryModel::getAminoAcidInsertions)
    }

    @GetMapping("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/{gene}", produces = ["text/x-fasta"])
    @LapisAlignedAminoAcidSequenceResponse
    fun getAlignedAminoAcidSequence(
        @PathVariable(name = "gene", required = true) gene: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return siloQueryModel.getGenomicSequence(request, SequenceType.ALIGNED, gene)
    }

    @PostMapping("$ALIGNED_AMINO_ACID_SEQUENCES_ROUTE/{gene}", produces = ["text/x-fasta"])
    @LapisAlignedAminoAcidSequenceResponse
    fun postAlignedAminoAcidSequence(
        @PathVariable(name = "gene", required = true) gene: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$ALIGNED_AMINO_ACID_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return siloQueryModel.getGenomicSequence(request, SequenceType.ALIGNED, gene)
    }

    private fun <Request : CommonSequenceFilters> getResponseAsCsv(
        request: Request,
        delimiter: Delimiter,
        getResponse: (request: Request) -> List<CsvRecord>,
    ): String {
        requestContext.filter = request
        val data = getResponse(request)

        if (data.isEmpty()) {
            return ""
        }

        val headers = data[0].getHeader()
        return csvWriter.write(headers, data, delimiter)
    }
}
