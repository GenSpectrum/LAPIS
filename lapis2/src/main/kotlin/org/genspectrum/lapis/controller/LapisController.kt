package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.genspectrum.lapis.controller.Delimiter.COMMA
import org.genspectrum.lapis.controller.Delimiter.TAB
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
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

const val SEQUENCE_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val INSERTIONS_REQUEST_SCHEMA = "InsertionsRequest"
const val SEQUENCE_REQUEST_SCHEMA = "SequenceRequest"

const val AGGREGATED_RESPONSE_SCHEMA = "AggregatedResponse"
const val DETAILS_RESPONSE_SCHEMA = "DetailsResponse"
const val NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA = "NucleotideMutationsResponse"
const val AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA = "AminoAcidMutationsResponse"
const val NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA = "NucleotideInsertionsResponse"
const val AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA = "AminoAcidInsertionsResponse"

const val NUCLEOTIDE_MUTATIONS_SCHEMA = "NucleotideMutations"
const val AMINO_ACID_MUTATIONS_SCHEMA = "AminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_SCHEMA = "NucleotideInsertions"
const val AMINO_ACID_INSERTIONS_SCHEMA = "AminoAcidInsertions"

const val ORDER_BY_FIELDS_SCHEMA = "OrderByFields"
const val LIMIT_SCHEMA = "Limit"
const val OFFSET_SCHEMA = "Offset"
const val FORMAT_SCHEMA = "DataFormat"

const val DETAILS_ENDPOINT_DESCRIPTION = "Returns the specified metadata fields of sequences matching the filter."
const val AGGREGATED_ENDPOINT_DESCRIPTION = "Returns the number of sequences matching the specified sequence filters"
const val NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by nucleotide mutations."
const val AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by amino acid mutations."
const val NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by nucleotide insertions."
const val AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION =
    "Returns a list of mutations along with the counts and proportions whose proportions are greater " +
        "than or equal to the specified minProportion. Only sequences matching the specified " +
        "sequence filters are considered."

const val AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION =
    "Returns a string of fasta formated amino acid sequences. Only sequences matching the specified " +
        "sequence filters are considered."
const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by." +
        "Fields specified here must either be \"count\" or also be present in \"fields\"."
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"
const val DETAILS_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by. Fields specified here must also be present in \"fields\"."
const val LIMIT_DESCRIPTION = "The maximum number of entries to return in the response"
const val OFFSET_DESCRIPTION = "The offset of the first entry to return in the response. " +
    "This is useful for pagination in combination with \"limit\"."
const val FORMAT_DESCRIPTION = "The data format of the response. " +
    "Alternatively, the data format can be specified by setting the \"Accept\"-header. When both are specified, " +
    "this parameter takes precedence."

const val AGGREGATED_ROUTE = "/aggregated"
const val DETAILS_ROUTE = "/details"
const val NUCLEOTIDE_MUTATIONS_ROUTE = "/nucleotideMutations"
const val AMINO_ACID_MUTATIONS_ROUTE = "/aminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_ROUTE = "/nucleotideInsertions"
const val AMINO_ACID_INSERTIONS_ROUTE = "/aminoAcidInsertions"
const val ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE = "/alignedNucleotideSequences"
const val AMINO_ACID_SEQUENCES_ROUTE = "/aminoAcidSequences"

@RestController
class LapisController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val csvWriter: CsvWriter,
) {
    @GetMapping(AGGREGATED_ROUTE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAggregatedResponse
    fun aggregated(
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
        @RequestParam
        dataFormat: String? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
        @RequestParam
        dataFormat: String? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        request: HttpServletRequest,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = "The format of the response.",
        )
        @RequestParam
        dataFormat: String? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        nucleotideMutations: List<NucleotideMutation>?,
        @RequestParam(required = false)
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam minProportion: Double?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by.",
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = DETAILS_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = DETAILS_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
        @RequestParam
        dataFormat: String? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "getDetailsAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun getDetailsAsCsv(
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = DETAILS_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = DETAILS_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = DETAILS_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = DETAILS_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
        @RequestParam
        offset: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$FORMAT_SCHEMA"),
            description = FORMAT_DESCRIPTION,
        )
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

    @GetMapping("$AMINO_ACID_SEQUENCES_ROUTE/{gene}", produces = ["text/x-fasta"])
    @LapisAminoAcidSequenceResponse
    fun getAminoAcidSequence(
        @PathVariable(name = "gene", required = true) gene: String,
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$LIMIT_SCHEMA"),
            description = LIMIT_DESCRIPTION,
        )
        @RequestParam
        limit: Int? = null,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$OFFSET_SCHEMA"),
            description = OFFSET_DESCRIPTION,
        )
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

    @PostMapping("$AMINO_ACID_SEQUENCES_ROUTE/{gene}", produces = ["text/x-fasta"])
    @LapisAminoAcidSequenceResponse
    fun postAminoAcidSequence(
        @PathVariable(name = "gene", required = true) gene: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$SEQUENCE_REQUEST_SCHEMA"))
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

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(description = AGGREGATED_ENDPOINT_DESCRIPTION)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisAggregatedResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisNucleotideMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisAminoAcidMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$DETAILS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisDetailsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisNucleotideInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisAminoAcidInsertionsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
)
private annotation class LapisAminoAcidSequenceResponse

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description = "Valid filters for sequence data. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
annotation class SequenceFilters
