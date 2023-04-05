package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.response.MutationData
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val MIN_PROPORTION_PROPERTY = "minProportion"

const val REQUEST_SCHEMA = "SequenceFilters"
const val REQUEST_SCHEMA_WITH_MIN_PROPORTION = "SequenceFiltersWithMinProportion"

private const val DEFAULT_MIN_PROPORTION = 0.05

@RestController
class LapisController(val siloQueryModel: SiloQueryModel) {

    @GetMapping("/aggregated")
    @LapisAggregatedResponse
    fun aggregated(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam
        sequenceFilters: Map<String, String>,
    ): AggregatedResponse {
        return siloQueryModel.aggregate(sequenceFilters)
    }

    @PostMapping("/aggregated")
    @LapisAggregatedResponse
    fun postAggregated(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"))
        @RequestBody
        sequenceFilters: Map<String, String>,
    ): AggregatedResponse {
        return siloQueryModel.aggregate(sequenceFilters)
    }

    @GetMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun getNucleotideMutations(
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"),
            explode = Explode.TRUE,
            style = ParameterStyle.FORM,
        )
        @RequestParam()
        sequenceFilters: Map<String, String>,
        @RequestParam(defaultValue = DEFAULT_MIN_PROPORTION.toString()) minProportion: Double,
    ): List<MutationData> {
        return siloQueryModel.computeMutationProportions(
            minProportion,
            sequenceFilters.filterKeys { it != MIN_PROPORTION_PROPERTY },
        )
    }

    @PostMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA_WITH_MIN_PROPORTION"))
        @RequestBody
        requestBody: Map<String, String>,
    ): List<MutationData> {
        val (minProportions, sequenceFilters) = requestBody.entries.partition { it.key == MIN_PROPORTION_PROPERTY }

        val maybeMinProportion = minProportions.getOrNull(0)?.value
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
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns the number of sequences matching the specified sequence filters",
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [Content(schema = Schema(implementation = AggregatedResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request",
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
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(schema = Schema(implementation = LapisHttpErrorResponse::class))],
        ),
    ],
)
private annotation class LapisNucleotideMutationsResponse
