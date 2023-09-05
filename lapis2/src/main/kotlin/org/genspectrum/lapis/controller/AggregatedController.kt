package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.controller.Delimiter.COMMA
import org.genspectrum.lapis.controller.Delimiter.TAB
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val AGGREGATED_RESPONSE_SCHEMA = "AggregatedResponse"
const val AGGREGATED_ENDPONT_DESCRIPTION = "Returns the number of sequences matching the specified sequence filters"
const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by." +
        "Fields specified here must either be \"count\" or also be present in \"fields\"."

@RestController
@RequestMapping("/aggregated")
class AggregatedController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    @Autowired val csvWriter: CsvWriterService,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
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
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        return LapisResponse(siloQueryModel.getAggregated(request))
    }

    @GetMapping(produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPONT_DESCRIPTION,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request
        return csvWriter.getResponseAsCsv(request, COMMA, siloQueryModel::getAggregated)
    }

    @GetMapping(produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPONT_DESCRIPTION,
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
    ): String {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            fields ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )
        requestContext.filter = request
        return csvWriter.getResponseAsCsv(request, TAB, siloQueryModel::getAggregated)
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAggregatedResponse
    @Operation(
        description = AGGREGATED_ENDPONT_DESCRIPTION,
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

    @PostMapping(produces = [TEXT_CSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPONT_DESCRIPTION,
        operationId = "postAggregatedAsCsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAggregatedAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        requestContext.filter = request
        return csvWriter.getResponseAsCsv(request, COMMA, siloQueryModel::getAggregated)
    }

    @PostMapping(produces = [TEXT_TSV_HEADER])
    @Operation(
        description = AGGREGATED_ENDPONT_DESCRIPTION,
        operationId = "postAggregatedAsTsv",
        responses = [ApiResponse(responseCode = "200")],
    )
    fun postAggregatedAsTsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        requestContext.filter = request
        return csvWriter.getResponseAsCsv(request, TAB, siloQueryModel::getAggregated)
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(description = "Returns the number of sequences matching the specified sequence filters")
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisAggregatedResponse
