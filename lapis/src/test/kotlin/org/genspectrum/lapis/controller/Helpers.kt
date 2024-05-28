package org.genspectrum.lapis.controller

import org.genspectrum.lapis.request.Field
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields

fun sequenceFiltersRequest(sequenceFilters: Map<String, String>) =
    SequenceFiltersRequest(
        sequenceFilters.mapValues { listOf(it.value) },
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
    )

fun mutationProportionsRequest(
    sequenceFilters: Map<String, String>,
    minProportion: Double?,
) = MutationProportionsRequest(
    sequenceFilters.mapValues { listOf(it.value) },
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    minProportion,
    emptyList(),
)

fun sequenceFiltersRequestWithFields(
    sequenceFilters: Map<String, String>,
    fields: List<String> = emptyList(),
) = SequenceFiltersRequestWithFields(
    sequenceFilters.mapValues { listOf(it.value) },
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    fields.map { Field(it) },
    emptyList(),
)

fun sequenceFiltersRequestWithArrayValuedFilters(
    sequenceFilters: Map<String, List<String>>,
    fields: List<String> = emptyList(),
) = SequenceFiltersRequestWithFields(
    sequenceFilters,
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    fields.map { Field(it) },
    emptyList(),
)
