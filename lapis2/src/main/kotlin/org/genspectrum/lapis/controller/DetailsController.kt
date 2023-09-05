package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
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
import org.genspectrum.lapis.response.DetailsData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val SEQUENCE_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val NUCLEOTIDE_MUTATIONS_SCHEMA = "NucleotideMutations"
const val AMINO_ACID_MUTATIONS_SCHEMA = "AminoAcidMutations"
const val ORDER_BY_FIELDS_SCHEMA = "OrderByFields"
const val LIMIT_SCHEMA = "Limit"
const val OFFSET_SCHEMA = "Offset"
const val FORMAT_SCHEMA = "DataFormat"
const val LIMIT_DESCRIPTION = "The maximum number of entries to return in the response"
const val OFFSET_DESCRIPTION = "The offset of the first entry to return in the response. " +
    "This is useful for pagination in combination with \"limit\"."
const val FORMAT_DESCRIPTION = "The data format of the response. " +
    "Alternatively, the data format can be specified by setting the \"Accept\"-header. When both are specified, " +
    "this parameter takes precedence."

const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val DETAILS_RESPONSE_SCHEMA = "DetailsResponse"
const val DETAILS_ENDPOINT_DESCRIPTION = "Returns the specified metadata fields of sequences matching the filter."
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"
const val DETAILS_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by. Fields specified here must also be present in \"fields\"."

@RestController
@RequestMapping("/details")
class DetailsController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    @Autowired val csvWriter: CsvWriterService,
) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
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
    ): LapisResponse<List<DetailsData>> {
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

        return LapisResponse(siloQueryModel.getDetails(request))
    }

    @GetMapping(produces = [TEXT_CSV_HEADER])
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
        return csvWriter.getResponseAsCsv(request, COMMA, siloQueryModel::getDetails)
    }

    @GetMapping(produces = [TEXT_TSV_HEADER])
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

        return csvWriter.getResponseAsCsv(request, TAB, siloQueryModel::getDetails)
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisDetailsResponse
    @Operation(
        description = DETAILS_ENDPOINT_DESCRIPTION,
        operationId = "postDetails",
    )
    fun postDetailsAsJson(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): LapisResponse<List<DetailsData>> {
        requestContext.filter = request

        return LapisResponse(siloQueryModel.getDetails(request))
    }

    @PostMapping(produces = [TEXT_CSV_HEADER])
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
        return csvWriter.getResponseAsCsv(request, COMMA, siloQueryModel::getDetails)
    }

    @PostMapping(produces = [TEXT_TSV_HEADER])
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
        return csvWriter.getResponseAsCsv(request, TAB, siloQueryModel::getDetails)
    }
}

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

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description = "Valid filters for sequence data. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
annotation class SequenceFilters
