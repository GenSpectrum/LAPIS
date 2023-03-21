package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import org.genspectrum.lapis.response.AggregatedResponse

data class SiloQuery<ResponseType>(val action: SiloAction<ResponseType>, val filterExpression: SiloFilterExpression)

sealed class SiloAction<ResponseType>(@JsonIgnore val typeReference: TypeReference<SiloQueryResponse<ResponseType>>) {
    companion object {
        fun aggregated(): SiloAction<AggregatedResponse> {
            return AggregatedAction("Aggregated")
        }
    }

    private data class AggregatedAction(val type: String) : SiloAction<AggregatedResponse>(
        object : TypeReference<SiloQueryResponse<AggregatedResponse>>() {},
    )
}

sealed class SiloFilterExpression(val type: String)

data class StringEquals(val column: String, val value: String) : SiloFilterExpression("StringEquals")

data class PangoLineageEquals(val pangoLineage: String) : SiloFilterExpression("PangoLineage")

data class NucleotideSymbolEquals(val position: Int, val symbol: String) : SiloFilterExpression("NucleotideEquals")

object True : SiloFilterExpression("True")

data class And(val children: List<SiloFilterExpression>) : SiloFilterExpression("And")
