package org.genspectrum.lapis.controller

import org.genspectrum.lapis.request.Field
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.response.MutationData
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

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
    sequenceFilters: Map<String, String> = emptyMap(),
    minProportion: Double? = null,
    fields: List<MutationsField> = emptyList(),
) = MutationProportionsRequest(
    sequenceFilters = sequenceFilters.mapValues { listOf(it.value) },
    nucleotideMutations = emptyList(),
    aaMutations = emptyList(),
    nucleotideInsertions = emptyList(),
    aminoAcidInsertions = emptyList(),
    fields = fields,
    minProportion = minProportion,
    orderByFields = emptyList(),
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

fun mutationData(
    mutation: String? = null,
    sequenceName: String? = null,
    position: Int? = null,
) = MutationData(
    mutation = mutation,
    count = null,
    coverage = null,
    proportion = null,
    sequenceName = sequenceName,
    mutationFrom = null,
    mutationTo = null,
    position = position,
)

fun MockHttpServletRequestBuilder.withFieldsQuery(fields: List<String>?) =
    fields?.fold(this) { request, field -> request.queryParam("fields", field) } ?: this

fun MockHttpServletRequestBuilder.withFieldsParam(fields: List<String>?) =
    fields?.fold(this) { request, field -> request.param("fields", field) } ?: this

fun getFieldsAsJsonPart(fields: List<String>?) =
    fields
        ?.joinToString { "\"$it\"" }
        ?.let { ", \"fields\": [$it]" }
        ?: ""
