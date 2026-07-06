package org.genspectrum.lapis.controller

import org.genspectrum.lapis.model.SiloQueryModel
import org.genspectrum.lapis.request.AminoAcidInsertion
import org.genspectrum.lapis.request.AminoAcidMutation
import org.genspectrum.lapis.request.CoOccurrencePosition
import org.genspectrum.lapis.request.CoOccurrenceRequest
import org.genspectrum.lapis.request.GetRequestSequenceFilters
import org.genspectrum.lapis.request.NucleotideInsertion
import org.genspectrum.lapis.request.NucleotideMutation
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.SPECIAL_REQUEST_PROPERTIES
import org.genspectrum.lapis.request.expandAndValidatePositions
import org.genspectrum.lapis.request.toOrderBySpec
import org.genspectrum.lapis.response.AggregatedCollection
import org.genspectrum.lapis.silo.coOccurrenceResponseFieldName

/**
 * Builds a [CoOccurrenceRequest] from the individual GET query parameters of a co-occurrence endpoint.
 */
fun buildCoOccurrenceRequest(
    sequenceFilters: GetRequestSequenceFilters?,
    positions: List<Int>,
    nucleotideMutations: List<NucleotideMutation>?,
    aminoAcidMutations: List<AminoAcidMutation>?,
    nucleotideInsertions: List<NucleotideInsertion>?,
    aminoAcidInsertions: List<AminoAcidInsertion>?,
    orderBy: List<OrderByField>?,
    limit: Int?,
    offset: Int?,
): CoOccurrenceRequest {
    if (positions.isEmpty()) {
        throw BadRequestException("'positions' must not be empty")
    }

    return CoOccurrenceRequest(
        sequenceFilters?.filter { !SPECIAL_REQUEST_PROPERTIES.contains(it.key) } ?: emptyMap(),
        nucleotideMutations ?: emptyList(),
        aminoAcidMutations ?: emptyList(),
        nucleotideInsertions ?: emptyList(),
        aminoAcidInsertions ?: emptyList(),
        positions.map { CoOccurrencePosition.Single(it) },
        orderBy.toOrderBySpec(),
        limit,
        offset,
    )
}

/**
 * Builds the [AggregatedCollection] for a co-occurrence response: it queries SILO for the co-occurrence of
 * symbols at the requested positions of [sequenceName], and prepares the dynamic (position-dependent) header
 * for the response, e.g. `["S:1", "S:421", "count"]`.
 */
fun getCoOccurrenceCollection(
    siloQueryModel: SiloQueryModel,
    request: CoOccurrenceRequest,
    sequenceName: String,
): AggregatedCollection {
    val positions = request.positions.expandAndValidatePositions()
    val fields = positions.map { coOccurrenceResponseFieldName(sequenceName, it) }

    return AggregatedCollection(
        records = siloQueryModel.getCoOccurrence(request, sequenceName),
        fields = fields,
    )
}
