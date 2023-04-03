package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.response.MutationProportion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val REQUEST_SCHEMA = "SequenceFilters"

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
    ): List<MutationProportion> {
        return siloQueryModel.computeMutationProportions(
            minProportion,
            sequenceFilters.filterKeys { it != "minProportion" },
        )
    }

    @PostMapping("/nucleotideMutations")
    @LapisNucleotideMutationsResponse
    fun postNucleotideMutations(
        @Parameter(schema = Schema(ref = "#/components/schemas/$REQUEST_SCHEMA"))
        @RequestBody
        requestBody: Map<String, String>,
    ): List<MutationProportion> {
        val (minProportions, sequenceFilters) = requestBody.entries.partition { it.key == "minProportion" }

        val maybeMinProportion = minProportions.getOrNull(0)?.value
        val minProportion = try {
            maybeMinProportion?.toDouble() ?: DEFAULT_MIN_PROPORTION
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid minProportion: Could not parse '$maybeMinProportion' to float.")
        }

        return siloQueryModel.computeMutationProportions(
            minProportion,
            sequenceFilters.associate { it.key to it.value },
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
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
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [Content(array = ArraySchema(schema = Schema(implementation = MutationProportion::class)))],
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
