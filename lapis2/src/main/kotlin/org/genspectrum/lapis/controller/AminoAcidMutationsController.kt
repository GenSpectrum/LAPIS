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
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.AminoAcidMutationResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA = "AminoAcidMutationsResponse"
const val AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by amino acid mutations."

@RestController
@RequestMapping("/aminoAcidMutations")
class AminoAcidMutationsController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    @Autowired val csvWriter: CsvWriterService,
) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidMutationsResponse
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "getAminoAcidMutations",
        responses = [ApiResponse(responseCode = "200")],
    )
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
    ): LapisResponse<List<AminoAcidMutationResponse>> {
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

        val result = siloQueryModel.computeAminoAcidMutationProportions(mutationProportionsRequest)
        return LapisResponse(result)
    }

    @GetMapping(produces = [TEXT_CSV_HEADER])
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
    ): String {
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

        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            COMMA,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @GetMapping(produces = [TEXT_TSV_HEADER])
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
    ): String {
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

        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            TAB,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @LapisAminoAcidMutationsResponse
    @Operation(
        description = AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION,
        operationId = "postAminoAcidMutations",
        responses = [ApiResponse(responseCode = "200")],
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

    @PostMapping(produces = [TEXT_CSV_HEADER])
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

        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            COMMA,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }

    @PostMapping(produces = [TEXT_TSV_HEADER])
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

        return csvWriter.getResponseAsCsv(
            mutationProportionsRequest,
            TAB,
            siloQueryModel::computeAminoAcidMutationProportions,
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = "Returns a list of mutations along with the counts and proportions whose proportions are greater " +
        "than or equal to the specified minProportion. Only sequences matching the specified " +
        "sequence filters are considered.",
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
    content = [
        Content(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_RESPONSE_SCHEMA")),
    ],
)
private annotation class LapisAminoAcidMutationsResponse
