package org.genspectrum.lapis.silo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import org.genspectrum.lapis.response.AggregatedResponse

data class SiloQuery<ResponseType>(val action: SiloAction<ResponseType>, val filter: SiloFilter) {
}

sealed class SiloAction<ResponseType>(@JsonIgnore val typeReference: TypeReference<SiloQueryResponse<ResponseType>>) {
    companion object {
        fun aggregated(): SiloAction<AggregatedResponse> {
            return AggregatedAction("Aggregated")
        }
    }

    private class AggregatedAction(val type: String) : SiloAction<AggregatedResponse>(
        object : TypeReference<SiloQueryResponse<AggregatedResponse>>() {}
    )
}

sealed class SiloFilter(val type: String) {}

data class StringEquals(val column: String, val value: String) : SiloFilter("StringEquals")
