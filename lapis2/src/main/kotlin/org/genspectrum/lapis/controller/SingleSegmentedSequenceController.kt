package org.genspectrum.lapis.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.genspectrum.lapis.config.REFERENCE_GENOME_APPLICATION_ARG_PREFIX
import org.genspectrum.lapis.config.ReferenceGenome
import org.genspectrum.lapis.logging.RequestContext
import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.silo.SequenceType
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    "Returns a string of fasta formatted aligned nucleotide sequences. Only sequences matching the " +
        "specified sequence filters are considered."

const val isSingleSegmentSequenceExpression = "#{'\${$REFERENCE_GENOME_APPLICATION_ARG_PREFIX}'.split(',').length == 1}"

@RestController
@ConditionalOnExpression(isSingleSegmentSequenceExpression)
class SingleSegmentedSequenceController(
    private val siloQueryModel: SiloQueryModel,
    private val requestContext: RequestContext,
    private val referenceGenome: ReferenceGenome,
) {

    @GetMapping(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE, produces = ["text/x-fasta"])
    @LapisAlignedSingleSegmentedNucleotideSequenceResponse
    fun getAlignedNucleotideSequences(
        @SequenceFilters
        @RequestParam
        sequenceFilters: Map<String, String>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$ORDER_BY_FIELDS_SCHEMA"),
            description = AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION,
        )
        @RequestParam
        orderBy: List<OrderByField>?,
        @Parameter(
            schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_MUTATIONS_SCHEMA"),
            explode = Explode.TRUE,
        )
        @RequestParam
        nucleotideMutations: List<NucleotideMutation>?,
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_MUTATIONS_SCHEMA"))
        @RequestParam
        aminoAcidMutations: List<AminoAcidMutation>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$NUCLEOTIDE_INSERTIONS_SCHEMA"))
        nucleotideInsertions: List<NucleotideInsertion>?,
        @RequestParam
        @Parameter(schema = Schema(ref = "#/components/schemas/$AMINO_ACID_INSERTIONS_SCHEMA"))
        aminoAcidInsertions: List<AminoAcidInsertion>?,
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
            referenceGenome.nucleotideSequences[0].name,
        )
    }

    @PostMapping(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE, produces = ["text/x-fasta"])
    @LapisAlignedSingleSegmentedNucleotideSequenceResponse
    fun postAlignedNucleotideSequence(
        @Parameter(schema = Schema(ref = "#/components/schemas/$SEQUENCE_REQUEST_SCHEMA"))
        @RequestBody
        request: SequenceFiltersRequest,
    ): String {
        requestContext.filter = request

        return siloQueryModel.getGenomicSequence(
            request,
            SequenceType.ALIGNED,
            referenceGenome.nucleotideSequences[0].name,
        )
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    description = ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION,
)
@ApiResponse(
    responseCode = "200",
    description = "OK",
)
private annotation class LapisAlignedSingleSegmentedNucleotideSequenceResponse
