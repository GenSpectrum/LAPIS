package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.ReferenceGenomeSchema
import org.genspectrum.lapis.controller.LapisMediaType.TEXT_X_FASTA_VALUE
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.ALL_NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.LapisAllNucleotideSequencesResponse
import org.genspectrum.lapis.openApi.LapisNucleotideSequenceResponse
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.NucleotideSequencesOrderByFields
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
import org.genspectrum.lapis.openApi.SEGMENTS_DESCRIPTION
import org.genspectrum.lapis.openApi.SEGMENT_SCHEMA
import org.genspectrum.lapis.openApi.Segment
import org.genspectrum.lapis.openApi.SequencesDataFormat
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.GetRequestSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SPECIAL_REQUEST_PROPERTIES
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithSegments
import org.genspectrum.lapis.response.SequencesStreamer
import org.genspectrum.lapis.silo.SequenceType
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

const val IS_MULTI_SEGMENT_SEQUENCE_EXPRESSION =
    "#{'\${$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX}'.split(',').length > 1}"

@RestController
@ConditionalOnExpression(IS_MULTI_SEGMENT_SEQUENCE_EXPRESSION)
@RequestMapping("/sample")
class MultiSegmentedSequenceController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val sequencesStreamer: SequencesStreamer,
    private val referenceGenomeSchema: ReferenceGenomeSchema,
) {
    @GetMapping(
        ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisAllNucleotideSequencesResponse(
        description = ALL_ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun getAllAlignedNucleotideSequences(
        @RequestParam
        @Parameter(
            array = ArraySchema(schema = Schema(ref = "#/components/schemas/$SEGMENT_SCHEMA")),
            description = SEGMENTS_DESCRIPTION,
        )
        segments: List<String>?,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @NucleotideSequencesOrderByFields
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
        @SequencesDataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = segments ?: referenceGenomeSchema.getNucleotideSequenceNames(),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = false,
                )
            }
    }

    @PostMapping(
        ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAllNucleotideSequencesResponse(
        description = ALL_ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun postAllAlignedNucleotideSequences(
        @Parameter(schema = Schema(ref = "#/components/schemas/$ALL_NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithSegments,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = request.segments,
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = false,
                )
            }
    }

    @GetMapping(
        "$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisNucleotideSequenceResponse(
        description = ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun getAlignedNucleotideSequence(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @NucleotideSequencesOrderByFields
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
        @SequencesDataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = listOf(segment),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = true,
                )
            }
    }

    @PostMapping(
        "$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisNucleotideSequenceResponse(
        description = ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun postAlignedNucleotideSequence(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.ALIGNED,
            sequenceNames = listOf(segment),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = true,
                )
            }
    }

    @GetMapping(
        UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisAllNucleotideSequencesResponse(
        description = ALL_UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun getAllUnalignedNucleotideSequences(
        @RequestParam
        @Parameter(
            array = ArraySchema(schema = Schema(ref = "#/components/schemas/$SEGMENT_SCHEMA")),
            description = SEGMENTS_DESCRIPTION,
        )
        segments: List<String>?,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @NucleotideSequencesOrderByFields
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
        @SequencesDataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.UNALIGNED,
            sequenceNames = segments ?: referenceGenomeSchema.getNucleotideSequenceNames(),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = false,
                )
            }
    }

    @PostMapping(
        UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE,
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisAllNucleotideSequencesResponse(
        description = ALL_UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun postAllUnalignedNucleotideSequences(
        @Parameter(schema = Schema(ref = "#/components/schemas/$ALL_NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequestWithSegments,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.UNALIGNED,
            sequenceNames = request.segments,
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = false,
                )
            }
    }

    @GetMapping(
        "$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
    )
    @LapisNucleotideSequenceResponse(
        description = UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun getUnalignedNucleotideSequence(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: GetRequestSequenceFilters?,
        @NucleotideSequencesOrderByFields
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
        @SequencesDataFormat
        @RequestParam
        dataFormat: String? = null,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        val request = SequenceFiltersRequest(
            sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
            nucleotideMutations ?: emptyList(),
            aminoAcidMutations ?: emptyList(),
            nucleotideInsertions ?: emptyList(),
            aminoAcidInsertions ?: emptyList(),
            orderBy ?: emptyList(),
            limit,
            offset,
        )

        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.UNALIGNED,
            sequenceNames = listOf(segment),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = true,
                )
            }
    }

    @PostMapping(
        "$UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}",
        produces = [TEXT_X_FASTA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_NDJSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @LapisNucleotideSequenceResponse(
        description = UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
    )
    fun postUnalignedNucleotideSequence(
        @PathVariable(name = "segment", required = true)
        @Segment
        segment: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
        @RequestHeader httpHeaders: HttpHeaders,
        response: HttpServletResponse,
    ) {
        requestContext.filter = request

        siloQueryModel.getGenomicSequence(
            sequenceFilters = request,
            sequenceType = SequenceType.UNALIGNED,
            sequenceNames = listOf(segment),
        )
            .also {
                sequencesStreamer.stream(
                    sequenceData = it,
                    response = response,
                    acceptHeaders = httpHeaders.accept,
                    singleSequenceEntry = true,
                )
            }
    }
}
