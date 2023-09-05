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
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.NucleotideMutationResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA = "NucleotideMutationsResponse"
const val NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by nucleotide mutations."

@RestController
@RequestMapping("/nucleotideMutations")
class NucleotideMutationsController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    @Autowired val csvWriter: CsvWriterService,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideMutationsResponse
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideMutations",
        responses = [ApiResponse(responseCode = "200")],
    )
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
    ): LapisResponse<List<NucleotideMutationResponse>> {
        val mutationProportionsRequest = MutationProportionsRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            minProportion,
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = mutationProportionsRequest

        val result = siloQueryModel.computeNucleotideMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @GetMapping(produces = [TEXT_CSV_HEADER])
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
    ): String {
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

        return csvWriter.getResponseAsCsv(request, COMMA, siloQueryModel::computeNucleotideMutationProportions)
    }

    @GetMapping(produces = [TEXT_TSV_HEADER])
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
    ): String {
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

        return csvWriter.getResponseAsCsv(request, TAB, siloQueryModel::computeNucleotideMutationProportions)
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisNucleotideMutationsResponse
    @Operation(
        description = NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideMutations",
        responses = [ApiResponse(responseCode = "200")],
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

    @PostMapping(produces = [TEXT_CSV_HEADER])
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
        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            COMMA,
            siloQueryModel::computeNucleotideMutationProportions,
        )
    }

    @PostMapping(produces = [TEXT_TSV_HEADER])
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
        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            TAB,
            siloQueryModel::computeNucleotideMutationProportions,
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns a list of mutations along with properties whose proportions are greater than or equal to " +
        "the specified minProportion. Only sequences matching the specified sequence filters are considered.",
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisNucleotideMutationsResponse
