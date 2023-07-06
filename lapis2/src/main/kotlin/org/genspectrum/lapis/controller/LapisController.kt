package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.auth.ACCESS_KEY_PROPERTY
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.silo.DetailsData
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val MIN_PROPORTION_PROPERTY = "minProportion"
const val FIELDS_PROPERTY = "fields"

const val SEQUENCE_FILTERS_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"
const val AGGREGATED_REQUEST_SCHEMA = "AggregatedPostRequest"
const val DETAILS_REQUEST_SCHEMA = "DetailsPostRequest"
const val AGGREGATED_RESPONSE_SCHEMA = "AggregatedResponse"
const val DETAILS_RESPONSE_SCHEMA = "DetailsResponse"

private const val DEFAULT_MIN_PROPORTION = 0.05
const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"

@RestController
class LapisController(private val siloQueryModel: SiloQueryModel, private val requestContext: RequestContext) {
    companion object {
        private val nonSequenceFilterFields =
            listOf(MIN_PROPORTION_PROPERTY, ACCESS_KEY_PROPERTY, FIELDS_PROPERTY)
    }

    @GetMapping("/aggregated")
    @LapisAggregatedResponse
    fun aggregated(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>,
        @Schema(description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION)
        @RequestParam(defaultValue = "")
        fields: List<String>,
    ): List<AggregationData> {
        requestContext.filter = sequenceFilters

        return siloQueryModel.aggregate(
            sequenceFilters.filterKeys { !nonSequenceFilterFields.contains(it) },
            fields,
        )
    }

    @PostMapping("/aggregated")
    @LapisAggregatedResponse
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$AGGREGATED_REQUEST_SCHEMA"))
        @RequestBody()
        request: SequenceFiltersRequestWithFields,
    ): List<AggregationData> {
        requestContext.filter = request.sequenceFilters

        return siloQueryModel.aggregate(
            request.sequenceFilters.filterKeys { !nonSequenceFilterFields.contains(it) },
            request.fields,
        )
    }

    @GetMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun getNucleotideMutations(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam()
        sequenceFilters: Map<String, String>,
        @RequestParam(defaultValue = DEFAULT_MIN_PROPORTION.toString()) minProportion: Double,
    ): List<MutationData> {
        requestContext.filter = sequenceFilters

        return siloQueryModel.computeMutationProportions(
            minProportion,
            sequenceFilters.filterKeys { !nonSequenceFilterFields.contains(it) },
        )
    }

    @PostMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        requestBody: Map<String, String>,
    ): List<MutationData> {
        requestContext.filter = requestBody

        val (nonSequenceFilters, sequenceFilters) = requestBody.entries.partition {
            nonSequenceFilterFields.contains(it.key)
        }

        val maybeMinProportion = nonSequenceFilters.find { it.key == MIN_PROPORTION_PROPERTY }?.value
        val minProportion = try {
            maybeMinProportion?.toDouble() ?: DEFAULT_MIN_PROPORTION
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid $MIN_PROPORTION_PROPERTY: Could not parse '$maybeMinProportion' to float.",
            )
        }

        return siloQueryModel.computeMutationProportions(
            minProportion,
            sequenceFilters.associate { it.key to it.value },
        )
    }

    @GetMapping("/details")
    @LapisDetailsResponse
    fun details(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>,
        @Schema(description = DETAILS_FIELDS_DESCRIPTION)
        @RequestParam(defaultValue = "")
        fields: List<String>,
    ): List<DetailsData> {
        requestContext.filter = sequenceFilters

        return siloQueryModel.getDetails(
            sequenceFilters.filterKeys { !nonSequenceFilterFields.contains(it) },
            fields,
        )
    }

    @PostMapping("/details")
    @LapisDetailsResponse
    fun postDetails(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody()
        request: SequenceFiltersRequestWithFields,
    ): List<DetailsData> {
        requestContext.filter = request.sequenceFilters

        return siloQueryModel.getDetails(
            request.sequenceFilters,
            request.fields,
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns the number of sequences matching the specified sequence filters",
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [
                Content(
                    array = ArraySchema(
                        schema = Schema(ref = "#/components/schemas/$AGGREGATED_RESPONSE_SCHEMA"),
                    ),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
    ],
)
private annotation class LapisAggregatedResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns a list of mutations along with properties whose proportions are greater than or equal to " +
        "the specified minProportion. Only sequences matching the specified sequence filters are considered.",
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [Content(array = ArraySchema(schema = Schema(implementation = MutationData::class)))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
    ],
)
private annotation class LapisNucleotideMutationsResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns the specified metadata fields of sequences matching the filter.",
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [
                Content(
                    array = ArraySchema(
                        schema = Schema(ref = "#/components/schemas/$DETAILS_RESPONSE_SCHEMA"),
                    ),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
    ],
)
private annotation class LapisDetailsResponse
