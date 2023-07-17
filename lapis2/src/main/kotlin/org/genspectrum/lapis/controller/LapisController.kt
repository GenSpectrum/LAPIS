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
import org.genspectrum.lapis.request.AMINO_ACID_MUTATIONS_PROPERTY
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.DEFAULT_MIN_PROPORTION
import org.genspectrum.lapis.request.FIELDS_PROPERTY
import org.genspectrum.lapis.request.MIN_PROPORTION_PROPERTY
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NUCLEOTIDE_MUTATIONS_PROPERTY
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.silo.DetailsData
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

const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"

@RestController
class LapisController(private val siloQueryModel: SiloQueryModel, private val requestContext: RequestContext) {
    companion object {
        private val nonSequenceFilterFields =
            listOf(
                MIN_PROPORTION_PROPERTY,
                ACCESS_KEY_PROPERTY,
                FIELDS_PROPERTY,
                NUCLEOTIDE_MUTATIONS_PROPERTY,
                AMINO_ACID_MUTATIONS_PROPERTY,
            )
    }

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
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
    ): List<AggregationData> {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !nonSequenceFilterFields.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            fields ?: emptyList(),
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
    ): List<MutationData> {
        val request = MutationProportionsRequest(
            sequenceFilters?.filter { !nonSequenceFilterFields.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            minProportion,
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

    @GetMapping("/details")
    @LapisDetailsResponse
    fun details(
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(description = AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION)
        @RequestParam
        fields: List<String>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"))
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
    ): List<DetailsData> {
        val request = SequenceFiltersRequestWithFields(
            sequenceFilters?.filter { !nonSequenceFilterFields.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            fields ?: emptyList(),
        )

        requestContext.filter = request

        return siloQueryModel.getDetails(request)
    }

    @PostMapping("/details")
    @LapisDetailsResponse
    fun postDetails(
        @Parameter(schema = Schema(ref = "#/components/schemas/$DETAILS_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithFields,
    ): List<DetailsData> {
        requestContext.filter = request

        return siloQueryModel.getDetails(request)
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

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Parameter(
    description = "Valid filters for sequence data. Only provide the fields that should be filtered by.",
    schema = Schema(ref = "#/components/schemas/$SEQUENCE_FILTERS_SCHEMA"),
    explode = Explode.TRUE,
    style = ParameterStyle.FORM,
)
private annotation class SequenceFilters
