package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.DEFAULT_MIN_PROPORTION
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.silo.DetailsData
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val SEQUENCE_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val AGGREGATED_RESPONSE_SCHEMA = "AggregatedResponse"
const val DETAILS_RESPONSE_SCHEMA = "DetailsResponse"

const val NUCLEOTIDE_MUTATIONS_SCHEMA = "NucleotideMutations"
const val AMINO_ACID_MUTATIONS_SCHEMA = "AminoAcidMutations"
const val ORDER_BY_FIELDS_SCHEMA = "OrderByFields"
const val LIMIT_SCHEMA = "Limit"
const val OFFSET_SCHEMA = "Offset"

const val DETAILS_ENDPOINT_DESCRIPTION = "Returns the specified metadata fields of sequences matching the filter."
const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"
const val LIMIT_DESCRIPTION = "The maximum number of entries to return in the response"
const val OFFSET_DESCRIPTION = "The offset of the first entry to return in the response. " +
    "This is useful for pagination in combination with \"limit\"."

@RestController
class LapisController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val csvWriter: CsvWriter,
) {
    @GetMapping("/aggregated")
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
            description = "The fields to order by." +
                " Fields specified here must either be \"count\" or also be present in \"fields\".",
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
    ): List<AggregationData> {
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

        return siloQueryModel.aggregate(request)
    }

    @PostMapping("/aggregated")
    @LapisAggregatedResponse
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): List<AggregationData> {
        requestContext.filter = request

        return siloQueryModel.aggregate(request)
    }

    @GetMapping("/nucleotideMutations")
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
        @RequestParam(defaultValue = DEFAULT_MIN_PROPORTION.toString())
        minProportion: Double,
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
    ): List<MutationData> {
        val request = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        return siloQueryModel.computeMutationProportions(request)
    }

    @PostMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        request: MutationProportionsRequest,
    ): List<MutationData> {
        requestContext.filter = request

        return siloQueryModel.computeMutationProportions(request)
    }

    @GetMapping("/details", produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "getDetails",
    )
    fun getDetailsAsJson(
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = DETAILS_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = "The fields of the response to order by." +
                " Fields specified here must also be present in \"fields\".",
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
    ): List<DetailsData> {
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

        return siloQueryModel.getDetails(request)
    }

    @GetMapping("/details", produces = ["text/csv"])
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "getDetailsAsCsv",
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
            description = "The fields of the response to order by." +
                " Fields specified here must also be present in \"fields\".",
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

        return getDetailsAsCsv(request)
    }

    @PostMapping("/details", produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetails",
    )
    fun postDetailsAsJson(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): List<DetailsData> {
        requestContext.filter = request

        return siloQueryModel.getDetails(request)
    }

    @PostMapping("/details", produces = ["text/csv"])
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetailsAsCsv",
    )
    fun postDetailsAsCsv(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): String {
        return getDetailsAsCsv(request)
    }

    private fun getDetailsAsCsv(request: SequenceFiltersRequestWithFields): String {
        requestContext.filter = request

        val data = siloQueryModel.getDetails(request)

        if (data.isEmpty()) {
            return ""
        }

        val headers = data[0].keys.toTypedArray<String>()
        return csvWriter.write(headers, data.map { it.asCsvRecord() })
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(description = "Returns the number of sequences matching the specified sequence filters")
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(
            array = ArraySchema(
                schema = Schema(ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA"),
            ),
        ),
    ],
)
private annotation class LapisAggregatedResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns a list of mutations along with properties whose proportions are greater than or equal to " +
        "the specified minProportion. Only sequences matching the specified sequence filters are considered.",
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [Content(array = ArraySchema(schema = Schema(implementation = MutationData::class)))],
)
private annotation class LapisNucleotideMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(
            array = ArraySchema(
                schema = Schema(ref = "#/components/schemas/$DETAILS_RESPONSE_SCHEMA"),
            ),
        ),
    ],
)
private annotation class LapisDetailsResponse

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description = "Valid filters for sequence data. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
private annotation class SequenceFilters
