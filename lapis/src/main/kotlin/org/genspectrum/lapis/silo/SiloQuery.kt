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

class AggregationDataTypeReference : TypeReference<AggregationData>()

class MutationDataTypeReference : TypeReference<MutationData>()

class AminoAcidMutationDataTypeReference : TypeReference<MutationData>()

class DetailsDataTypeReference : TypeReference<DetailsData>()

class InsertionDataTypeReference : TypeReference<InsertionData>()

class SequenceDataTypeReference : TypeReference<SequenceData>()

interface CommonActionFields {
    val orderByFields: List<OrderByField>
    val limit: Int?
    val offset: Int?
    val randomize: Boolean?
}

const val ORDER_BY_RANDOM_FIELD_NAME = "random"

sealed class SiloAction<ResponseType>(
    @JsonIgnore val typeReference: TypeReference<ResponseType>,
    @JsonIgnore val cacheable: Boolean,
) : CommonActionFields {
    companion object {
        fun aggregated(
            groupByFields: List<String> = emptyList(),
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<AggregationData> =
            AggregatedAction(
                groupByFields = groupByFields,
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun mutations(
            minProportion: Double? = null,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<MutationData> =
            MutationsAction(
                minProportion = minProportion,
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun aminoAcidMutations(
            minProportion: Double? = null,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<MutationData> =
            AminoAcidMutationsAction(
                minProportion = minProportion,
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun details(
            fields: List<String> = emptyList(),
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<DetailsData> =
            DetailsAction(
                fields = fields,
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun nucleotideInsertions(
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            NucleotideInsertionsAction(
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun aminoAcidInsertions(
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<InsertionData> =
            AminoAcidInsertionsAction(
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        fun genomicSequence(
            type: SequenceType,
            sequenceName: String,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<SequenceData> =
            SequenceAction(
                type = type,
                sequenceName = sequenceName,
                orderByFields = getNonRandomizedOrderByFields(orderByFields),
                limit = limit,
                offset = offset,
                randomize = getRandomize(orderByFields),
            )

        private fun getRandomize(orderByFields: List<OrderByField>) =
            orderByFields.any { it.field == ORDER_BY_RANDOM_FIELD_NAME }

        private fun getNonRandomizedOrderByFields(orderByFields: List<OrderByField>) =
            orderByFields.filter { it.field != ORDER_BY_RANDOM_FIELD_NAME }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AggregatedAction(
        val groupByFields: List<String>,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Aggregated",
    ) : SiloAction<AggregationData>(AggregationDataTypeReference(), cacheable = true)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class MutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Mutations",
    ) : SiloAction<MutationData>(MutationDataTypeReference(), cacheable = true)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AminoAcidMutationsAction(
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "AminoAcidMutations",
    ) : SiloAction<MutationData>(AminoAcidMutationDataTypeReference(), cacheable = true)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class DetailsAction(
        val fields: List<String> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Details",
    ) : SiloAction<DetailsData>(DetailsDataTypeReference(), cacheable = false)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class NucleotideInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "Insertions",
    ) : SiloAction<InsertionData>(InsertionDataTypeReference(), cacheable = true)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AminoAcidInsertionsAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: String = "AminoAcidInsertions",
    ) : SiloAction<InsertionData>(InsertionDataTypeReference(), cacheable = true)

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class SequenceAction(
        override val orderByFields: List<OrderByField> = emptyList(),
        override val randomize: Boolean? = null,
        override val limit: Int? = null,
        override val offset: Int? = null,
        val type: SequenceType,
        val sequenceName: String,
    ) : SiloAction<SequenceData>(SequenceDataTypeReference(), cacheable = false)
}

sealed class SiloFilterExpression(val type: String)

data class StringEquals(val column: String, val value: String?) : SiloFilterExpression("StringEquals")

data class BooleanEquals(val column: String, val value: Boolean?) : SiloFilterExpression("BooleanEquals")

data class PangoLineageEquals(val column: String, val value: String?, val includeSublineages: Boolean) :
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

data class IntEquals(val column: String, val value: Int?) : SiloFilterExpression("IntEquals")

data class IntBetween(val column: String, val from: Int?, val to: Int?) : SiloFilterExpression("IntBetween")

data class FloatEquals(val column: String, val value: Double?) : SiloFilterExpression("FloatEquals")

data class FloatBetween(val column: String, val from: Double?, val to: Double?) : SiloFilterExpression("FloatBetween")

enum class SequenceType {
    @JsonProperty("Fasta")
    UNALIGNED,

    @JsonProperty("FastaAligned")
    ALIGNED,
}
