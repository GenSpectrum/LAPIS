package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.genspectrum.lapis.response.AggregationData
import org.genspectrum.lapis.response.MutationData
import java.time.LocalDate

typealias DetailsData = Map<String, JsonNode>

data class SiloQuery<ResponseType>(val action: SiloAction<ResponseType>, val filterExpression: SiloFilterExpression)

sealed class SiloAction<ResponseType>(@JsonIgnore val typeReference: TypeReference<SiloQueryResponse<ResponseType>>) {
    companion object {
        fun aggregated(groupByFields: List<String> = emptyList()): SiloAction<List<AggregationData>> =
            AggregatedAction("Aggregated", groupByFields)

        fun mutations(minProportion: Double? = null): SiloAction<List<MutationData>> =
            MutationsAction("Mutations", minProportion)

        fun details(fields: List<String> = emptyList()): SiloAction<List<DetailsData>> =
            DetailsAction("Details", fields)
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class AggregatedAction(val type: String, val groupByFields: List<String>) :
        SiloAction<List<AggregationData>>(object : TypeReference<SiloQueryResponse<List<AggregationData>>>() {})

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class MutationsAction(val type: String, val minProportion: Double?) :
        SiloAction<List<MutationData>>(object : TypeReference<SiloQueryResponse<List<MutationData>>>() {})

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private data class DetailsAction(val type: String, val fields: List<String> = emptyList()) :
        SiloAction<List<DetailsData>>(
            object :
                TypeReference<SiloQueryResponse<List<DetailsData>>>() {},
        )
}

sealed class SiloFilterExpression(val type: String)

data class StringEquals(val column: String, val value: String) : SiloFilterExpression("StringEquals")

data class PangoLineageEquals(val column: String, val value: String, val includeSublineages: Boolean) :
    SiloFilterExpression("PangoLineage")

data class NucleotideSymbolEquals(val position: Int, val symbol: String) : SiloFilterExpression("NucleotideEquals")

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
