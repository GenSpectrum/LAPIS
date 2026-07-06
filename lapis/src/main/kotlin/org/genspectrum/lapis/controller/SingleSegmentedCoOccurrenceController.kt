package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_VALUE
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_TSV_VALUE
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.DataFormat
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnExpression(SHOW_SINGLE_SEGMENTED_CONTROLLER)
@RequestMapping("/component")
class SingleSegmentedCoOccurrenceController(
    private val siloQueryModel: SiloQueryModel,
    private val lapisResponseStreamer: LapisResponseStreamer,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
    @GetMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Operation(description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION)
    fun getNucleotideCoOccurrenceAsJson(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam
        positions: List<Int>,
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
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @GetMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [TEXT_CSV_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "getSingleSegmentNucleotideCoOccurrenceAsCsv",
    )
    fun getNucleotideCoOccurrenceAsCsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam
        positions: List<Int>,
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
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = COMMA, acceptHeader = httpHeaders.accept),
        )
    }

    @GetMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [TEXT_TSV_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "getSingleSegmentNucleotideCoOccurrenceAsTsv",
    )
    fun getNucleotideCoOccurrenceAsTsv(
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @RequestParam
        positions: List<Int>,
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
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = TAB, acceptHeader = httpHeaders.accept),
        )
    }

    @PostMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @Operation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "postSingleSegmentNucleotideCoOccurrence",
    )
    fun postNucleotideCoOccurrence(
        @Parameter(description = "The sequence filters, positions, and other options for the co-occurrence query.")
        @RequestBody
        request: CoOccurrenceRequest,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Json,
        )
    }

    @PostMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [TEXT_CSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "postSingleSegmentNucleotideCoOccurrenceAsCsv",
    )
    fun postNucleotideCoOccurrenceAsCsv(
        @RequestBody
        request: CoOccurrenceRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = COMMA, acceptHeader = httpHeaders.accept),
        )
    }

    @PostMapping(
        NUCLEOTIDE_CO_OCCURRENCE_ROUTE,
        produces = [TEXT_TSV_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @StringResponseOperation(
        description = NUCLEOTIDE_CO_OCCURRENCE_ENDPOINT_DESCRIPTION,
        operationId = "postSingleSegmentNucleotideCoOccurrenceAsTsv",
    )
    fun postNucleotideCoOccurrenceAsTsv(
        @RequestBody
        request: CoOccurrenceRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        lapisResponseStreamer.streamData(
            request = request,
            getData = { getCoOccurrenceCollection(siloQueryModel, it, soleSegmentName()) },
            response = response,
            responseFormat = ResponseFormat.Csv(delimiter = TAB, acceptHeader = httpHeaders.accept),
        )
    }

    private fun soleSegmentName() = referenceGenomeSchema.nucleotideSequences[0].name
}
