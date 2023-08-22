package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import org.genspectrum.lapis.request.OrderByField
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.DetailsData
import org.genspectrum.lapis.response.MutationData
import java.time.LocalDate

data class SiloQuery<ResponseType>(val action: SiloAction<ResponseType>, val filterExpression: SiloFilterExpression)

class AggregationDataTypeReference : TypeReference<SiloQueryResponse<List<AggregationData>>>()
class MutationDataTypeReference : TypeReference<SiloQueryResponse<List<MutationData>>>()
class DetailsDataTypeReference : TypeReference<SiloQueryResponse<List<DetailsData>>>()

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
        ): SiloAction<List<AggregationData>> =
            AggregatedAction("Aggregated", groupByFields, orderByFields, limit, offset)

        fun mutations(
            minProportion: Double? = null,
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<MutationData>> = MutationsAction("Mutations", minProportion, orderByFields, limit, offset)

        fun details(
            fields: List<String> = emptyList(),
            orderByFields: List<OrderByField> = emptyList(),
            limit: Int? = null,
            offset: Int? = null,
        ): SiloAction<List<DetailsData>> = DetailsAction("Details", fields, orderByFields, limit, offset)
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AggregatedAction(
        val type: String,
        val groupByFields: List<String>,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<List<AggregationData>>(AggregationDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class MutationsAction(
        val type: String,
        val minProportion: Double?,
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<List<MutationData>>(MutationDataTypeReference())

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class DetailsAction(
        val type: String,
        val fields: List<String> = emptyList(),
        override val orderByFields: List<OrderByField> = emptyList(),
        override val limit: Int? = null,
        override val offset: Int? = null,
    ) : SiloAction<List<DetailsData>>(DetailsDataTypeReference())
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

object True : SiloFilterExpression("True")

data class And(val children: List<SiloFilterExpression>) : SiloFilterExpression("And")

data class Or(val children: List<SiloFilterExpression>) : SiloFilterExpression("Or")

data class Not(val child: SiloFilterExpression) : SiloFilterExpression("Not")

data class Maybe(val child: SiloFilterExpression) : SiloFilterExpression("Maybe")

data class NOf(val numberOfMatchers: Int, val matchExactly: Boolean, val children: List<SiloFilterExpression>) :
    SiloFilterExpression("N-Of")

data class IntEquals(val column: String, val value: Int) : SiloFilterExpression("IntEquals")

data class IntBetween(val column: String, val from: Int?, val to: Int?) : SiloFilterExpression("IntBetween")

data class FloatEquals(val column: String, val value: Double) : SiloFilterExpression("FloatEquals")

data class FloatBetween(val column: String, val from: Double?, val to: Double?) : SiloFilterExpression("FloatBetween")
