package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.config.REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.openApi.AminoAcidInsertions
import org.genspectrum.lapis.openApi.AminoAcidMutations
import org.genspectrum.lapis.openApi.LapisAlignedMultiSegmentedNucleotideSequenceResponse
import org.genspectrum.lapis.openApi.Limit
import org.genspectrum.lapis.openApi.NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA
import org.genspectrum.lapis.openApi.NucleotideInsertions
import org.genspectrum.lapis.openApi.NucleotideMutations
import org.genspectrum.lapis.openApi.NucleotideSequencesOrderByFields
import org.genspectrum.lapis.openApi.Offset
import org.genspectrum.lapis.openApi.PrimitiveFieldFilters
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.silo.SequenceType
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val IS_MULTI_SEGMENT_SEQUENCE_EXPRESSION =
    "#{'\${$REFERENCE_GENOME_SEGMENTS_APPLICATION_ARG_PREFIX}'.split(',').length > 1}"

@RestController
@ConditionalOnExpression(IS_MULTI_SEGMENT_SEQUENCE_EXPRESSION)
class MultiSegmentedSequenceController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
) {
    @GetMapping("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}", produces = ["text/x-fasta"])
    @LapisAlignedMultiSegmentedNucleotideSequenceResponse
    fun getAlignedNucleotideSequence(
        @PathVariable(name = "segment", required = true) segment: String,
        @PrimitiveFieldFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
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
    ): String {
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

        return siloQueryModel.getGenomicSequence(
            request,
            SequenceType.ALIGNED,
            segment,
        )
    }

    @PostMapping("$ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE/{segment}", produces = ["text/x-fasta"])
    @LapisAlignedMultiSegmentedNucleotideSequenceResponse
    fun postAlignedNucleotideSequence(
        @PathVariable(name = "segment", required = true) segment: String,
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return siloQueryModel.getGenomicSequence(
            request,
            SequenceType.ALIGNED,
            segment,
        )
    }
}
