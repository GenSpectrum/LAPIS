package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.InsertionData
import org.genspectrum.lapis.response.MutationData
import org.genspectrum.lapis.response.SequenceData
import java.time.LocalDate

data class SiloQuery<ResponseType>(val action: SiloAction<ResponseType>, val filterExpression: SiloFilterExpression)

class AggregationDataTypeReference : TypeReference<SiloQueryResponse<List<AggregationData>>>()

class MutationDataTypeReference : TypeReference<SiloQueryResponse<List<MutationData>>>()

class AminoAcidMutationDataTypeReference : TypeReference<SiloQueryResponse<List<MutationData>>>()

class DetailsDataTypeReference : TypeReference<SiloQueryResponse<List<DetailsData>>>()

class InsertionDataTypeReference : TypeReference<SiloQueryResponse<List<InsertionData>>>()

class SequenceDataTypeReference : TypeReference<SiloQueryResponse<List<SequenceData>>>()

interface CommonActionFields {
    val orderByFields: List<OrderByField>
    val limit: Int?
    val offset: Int?
}

sealed class SiloAction<ResponseType>(
    @JsonIgnore val typeReference: TypeReference<SiloQueryResponse<ResponseType>>,
) : CommonActionFields {
    companion object {
        fun aggregated(
            groupByFields: List<String> = emptyList(),
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<AggregationData>> = AggregatedAction(groupByFields, orderByFields, limit, offset)

        fun mutations(
            minProportion: Double? = null,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<MutationData>> =
            MutationsAction(
                minProportion,
                orderByFields,
                limit,
                offset,
            )

        fun aminoAcidMutations(
            minProportion: Double? = null,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<MutationData>> =
            AminoAcidMutationsAction(
                minProportion,
                orderByFields,
                limit,
                offset,
            )

        fun details(
            fields: List<String> = emptyList(),
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<DetailsData>> = DetailsAction(fields, orderByFields, limit, offset)

        fun nucleotideInsertions(
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<InsertionData>> = NucleotideInsertionsAction(orderByFields, limit, offset)

        fun aminoAcidInsertions(
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<InsertionData>> = AminoAcidInsertionsAction(orderByFields, limit, offset)

        fun genomicSequence(
            type: SequenceType,
            sequenceName: String,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<SequenceData>> = SequenceAction(orderByFields, limit, offset, type, sequenceName)
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AggregatedAction(
        val groupByFields: List<String>,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Aggregated",
    ) : SiloAction<List<AggregationData>>(AggregationDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class MutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Mutations",
    ) : SiloAction<List<MutationData>>(MutationDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AminoAcidMutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "AminoAcidMutations",
    ) : SiloAction<List<MutationData>>(AminoAcidMutationDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class DetailsAction(
        val fields: List<String> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Details",
    ) : SiloAction<List<DetailsData>>(DetailsDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class NucleotideInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Insertions",
    ) : SiloAction<List<InsertionData>>(InsertionDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AminoAcidInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "AminoAcidInsertions",
    ) : SiloAction<List<InsertionData>>(InsertionDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class SequenceAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: SequenceType,
        val sequenceName: String,
    ) : SiloAction<List<SequenceData>>(SequenceDataTypeReference())
}

sealed class SiloFilterExpression(val type: String)

data class StringEquals(val column: String, val value: String) : SiloFilterExpression("StringEquals")

data class PangoLineageEquals(val column: String, val value: String, val includeSublineages: Boolean) :
    SiloFilterExpression("PangoLineage")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NucleotideSymbolEquals(val sequenceName: String?, val position: Int, val symbol: String) :
    SiloFilterExpression("NucleotideEquals")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HasNucleotideMutation(val sequenceName: String?, val position: Int) :
    SiloFilterExpression("HasNucleotideMutation")

data class AminoAcidSymbolEquals(val sequenceName: String, val position: Int, val symbol: String) :
    SiloFilterExpression("AminoAcidEquals")

data class HasAminoAcidMutation(val sequenceName: String, val position: Int) :
    SiloFilterExpression("HasAminoAcidMutation")

data class DateBetween(val column: String, val from: LocalDate?, val to: LocalDate?) :
    SiloFilterExpression("DateBetween")

data class NucleotideInsertionContains(val position: Int, val value: String) : SiloFilterExpression("InsertionContains")

data class AminoAcidInsertionContains(val position: Int, val value: String, val sequenceName: String) :
    SiloFilterExpression(
        "AminoAcidInsertionContains",
    )

data object True : SiloFilterExpression("True")

data class And(val children: List<SiloFilterExpression>) : SiloFilterExpression("And") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())
}

data class Or(val children: List<SiloFilterExpression>) : SiloFilterExpression("Or") {
    constructor(vararg children: SiloFilterExpression) : this(children.toList())
}

data class Not(val child: SiloFilterExpression) : SiloFilterExpression("Not")

data class Maybe(val child: SiloFilterExpression) : SiloFilterExpression("Maybe")

data class NOf(val numberOfMatchers: Int, val matchExactly: Boolean, val children: List<SiloFilterExpression>) :
    SiloFilterExpression("N-Of")

data class IntEquals(val column: String, val value: Int) : SiloFilterExpression("IntEquals")

data class IntBetween(val column: String, val from: Int?, val to: Int?) : SiloFilterExpression("IntBetween")

data class FloatEquals(val column: String, val value: Double) : SiloFilterExpression("FloatEquals")

data class FloatBetween(val column: String, val from: Double?, val to: Double?) : SiloFilterExpression("FloatBetween")

enum class SequenceType {
    @JsonProperty("Fasta")
    UNALIGNED,

    @JsonProperty("FastaAligned")
    ALIGNED,
}
