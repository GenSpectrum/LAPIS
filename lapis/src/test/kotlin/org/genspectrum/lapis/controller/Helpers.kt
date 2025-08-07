package org.genspectrum.lapis.controller

import org.genspectrum.lapis.request.Field
import org.genspectrum.lapis.request.MRCASequenceFiltersRequest
import org.genspectrum.lapis.request.MutationProportionsRequest
import org.genspectrum.lapis.request.MutationsField
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.request.PhyloTreeSequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequest
import org.genspectrum.lapis.request.SequenceFiltersRequestWithFields
import org.genspectrum.lapis.request.SequenceFiltersRequestWithGenes
import org.genspectrum.lapis.request.SequenceFiltersRequestWithSegments
import org.genspectrum.lapis.response.MutationData
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

fun sequenceFiltersRequest(
    sequenceFilters: Map<String, String>,
    orderByFields: List<OrderByField> = emptyList(),
) = SequenceFiltersRequest(
    sequenceFilters = sequenceFilters.mapValues { listOf(it.value) },
    nucleotideMutations = emptyList(),
    aminoAcidMutations = emptyList(),
    nucleotideInsertions = emptyList(),
    aminoAcidInsertions = emptyList(),
    orderByFields = orderByFields,
)

fun mutationProportionsRequest(
    sequenceFilters: Map<String, String> = emptyMap(),
    minProportion: Double? = null,
    fields: List<MutationsField> = emptyList(),
) = MutationProportionsRequest(
    sequenceFilters = sequenceFilters.mapValues { listOf(it.value) },
    nucleotideMutations = emptyList(),
    aminoAcidMutations = emptyList(),
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

fun phyloTreeSequenceFiltersRequest(
    sequenceFilters: Map<String, String>,
    phyloTreeField: String,
) = PhyloTreeSequenceFiltersRequest(
    sequenceFilters.mapValues { listOf(it.value) },
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    null,
    null,
    phyloTreeField = phyloTreeField
)

fun mrcaSequenceFiltersRequest(
    sequenceFilters: Map<String, String>,
    phyloTreeField: String,
    printNodesNotInTree: Boolean = false,
) = MRCASequenceFiltersRequest(
    sequenceFilters.mapValues { listOf(it.value) },
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    emptyList(),
    null,
    null,
    phyloTreeField = phyloTreeField,
    printNodesNotInTree = printNodesNotInTree,
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

fun sequenceFiltersRequestWithSegments(
    sequenceFilters: Map<String, String>,
    segments: List<String> = emptyList(),
) = SequenceFiltersRequestWithSegments(
    sequenceFilters = sequenceFilters.mapValues { listOf(it.value) },
    nucleotideMutations = emptyList(),
    aminoAcidMutations = emptyList(),
    nucleotideInsertions = emptyList(),
    aminoAcidInsertions = emptyList(),
    segments = segments,
    orderByFields = emptyList(),
)

fun sequenceFiltersRequestWithGenes(
    sequenceFilters: Map<String, String>,
    genes: List<String> = emptyList(),
) = SequenceFiltersRequestWithGenes(
    sequenceFilters = sequenceFilters.mapValues { listOf(it.value) },
    nucleotideMutations = emptyList(),
    aminoAcidMutations = emptyList(),
    nucleotideInsertions = emptyList(),
    aminoAcidInsertions = emptyList(),
    genes = genes,
    orderByFields = emptyList(),
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

fun MockHttpServletRequestBuilder.withPhyloTreeFieldQuery(phyloTreeField: String?) =
    phyloTreeField?.let { this.queryParam("phyloTreeField", it) } ?: this

fun MockHttpServletRequestBuilder.withFieldsParam(fields: List<String>?) =
    fields?.fold(this) { request, field -> request.param("fields", field) } ?: this

fun MockHttpServletRequestBuilder.withPhyloTreeFieldParam(phyloTreeField: String?) =
    phyloTreeField?.let { this.param("phyloTreeField", it) } ?: this

fun getFieldsAsJsonPart(fields: List<String>?) =
    fields
        ?.joinToString { "\"$it\"" }
        ?.let { ", \"fields\": [$it]" }
        ?: ""

fun getPhyloTreeFieldAsJsonPart(phyloTreeField: String?) = phyloTreeField?.let { ", \"phyloTreeField\": \"$it\"" } ?: ""
