package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.CO_OCCURRENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.CoOccurrenceOrderByFields
import org.genspectrum.lapis.openApi.CoOccurrencePositionsParam
import org.genspectrum.lapis.openApi.DataFormat
import org.genspectrum.lapis.openApi.LapisCoOccurrenceResponse
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
import org.genspectrum.lapis.openApi.Segment
import org.genspectrum.lapis.openApi.StringResponseOperation
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.CoOccurrenceRequest
import org.genspectrum.lapis.request.GetRequestSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.Delimiter.COMMA
import org.genspectrum.lapis.response.Delimiter.TAB
import org.genspectrum.lapis.response.LapisResponseStreamer
import org.genspectrum.lapis.response.ResponseFormat
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnExpression(IS_MULTI_SEGMENT_SEQUENCE_EXPRESSION)
@RequestMapping("/component")
class MultiSegmentedCoOccurrenceController(
    private val siloQueryModel: SiloQueryModel,
    private val lapisResponseStreamer: LapisResponseStreamer,
) {
    @GetMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @LapisCoOccurrenceResponse(description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION)
    fun getNucleotideCoOccurrenceAsJson(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @CoOccurrencePositionsParam
        @RequestParam
        positions: List<Int>,
        @CoOccurrenceOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        response: HttpServletResponse,
    ) {
        val request = buildCoOccurrenceRequest(
            sequenceFilters,
            positions,
            nucleotideMutations,
            aminoAcidMutations,
            nucleotideInsertions,
            aminoAcidInsertions,
            orderBy,
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [TEXT_CSV_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideCoOccurrenceAsCsv",
    )
    fun getNucleotideCoOccurrenceAsCsv(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @CoOccurrencePositionsParam
        @RequestParam
        positions: List<Int>,
        @CoOccurrenceOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = buildCoOccurrenceRequest(
            sequenceFilters,
            positions,
            nucleotideMutations,
            aminoAcidMutations,
            nucleotideInsertions,
            aminoAcidInsertions,
            orderBy,
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = COMMA, acceptHeader = httpHeaders.accept),
        )
    }

    @GetMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [TEXT_TSV_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "getNucleotideCoOccurrenceAsTsv",
    )
    fun getNucleotideCoOccurrenceAsTsv(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @CoOccurrencePositionsParam
        @RequestParam
        positions: List<Int>,
        @CoOccurrenceOrderByFields
        @RequestParam
        orderBy: List<OrderByField>?,
        @NucleotideMutations
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @AminoAcidMutations
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @NucleotideInsertions
        @RequestParam
        nucleotideInsertions: List<NucleotideInsertion>?,
        @AminoAcidInsertions
        @RequestParam
        aminoAcidInsertions: List<AminoAcidInsertion>?,
        @Limit
        @RequestParam
        limit: Int? = null,
        @Offset
        @RequestParam
        offset: Int? = null,
        @DataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = buildCoOccurrenceRequest(
            sequenceFilters,
            positions,
            nucleotideMutations,
            aminoAcidMutations,
            nucleotideInsertions,
            aminoAcidInsertions,
            orderBy,
            limit,
            offset,
        )

        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = TAB, acceptHeader = httpHeaders.accept),
        )
    }

    @PostMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisCoOccurrenceResponse(description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION)
    @Operation(
        operationId = "postNucleotideCoOccurrence",
    )
    fun postNucleotideCoOccurrence(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$CO_OCCURRENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: CoOccurrenceRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideCoOccurrenceAsCsv",
    )
    fun postNucleotideCoOccurrenceAsCsv(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @RequestBody
        request: CoOccurrenceRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = COMMA, acceptHeader = httpHeaders.accept),
        )
    }

    @PostMapping(
        "$NUCLEOTIDE_CO_OCCURRENCE_ROUTE/{segment}",
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "postNucleotideCoOccurrenceAsTsv",
    )
    fun postNucleotideCoOccurrenceAsTsv(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @RequestBody
        request: CoOccurrenceRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, segment) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = TAB, acceptHeader = httpHeaders.accept),
        )
    }
}
