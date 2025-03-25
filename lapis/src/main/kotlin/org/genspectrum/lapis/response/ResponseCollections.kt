package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import java.util.stream.Stream

class AggregatedCollection(
    override val records: Stream<AggregationData>,
    private val fields: List<String>,
) : RecordCollection<AggregationData> {
    override fun getHeader() = fields + COUNT_PROPERTY

    override fun mapToCsvValuesList(value: AggregationData): List<String?> =
        fields
            .map { value.fields[it]?.toCsvValue() }
            .plus(value.count.toString())
}

class DetailsCollection(
    override val records: Stream<DetailsData>,
    private val fields: List<String>,
) : RecordCollection<DetailsData> {
    override fun getHeader() = fields

    override fun mapToCsvValuesList(value: DetailsData): List<String?> =
        fields
            .map { value[it]?.toCsvValue() }
}

private fun JsonNode.toCsvValue() =
    when (this) {
        is NullNode -> null
        else -> asText()
    }

class MutationsCollection(
    override val records: Stream<MutationResponse>,
) : RecordCollection<MutationResponse> {
    override fun getHeader() =
        listOf(
            "mutation",
            "count",
            "coverage",
            "proportion",
            "sequenceName",
            "mutationFrom",
            "mutationTo",
            "position",
        )

    override fun mapToCsvValuesList(value: MutationResponse): List<String?> =
        listOf(
            value.mutation,
            value.count.toString(),
            value.coverage.toString(),
            value.proportion.toString(),
            value.sequenceName ?: "",
            value.mutationFrom,
            value.mutationTo,
            value.position.toString(),
        )
}

class InsertionsCollection(
    override val records: Stream<InsertionResponse>,
) : RecordCollection<InsertionResponse> {
    override fun getHeader() =
        listOf(
            "insertion",
            "count",
            "insertedSymbols",
            "position",
            "sequenceName",
        )

    override fun mapToCsvValuesList(value: InsertionResponse): List<String?> =
        listOf(
            value.insertion,
            value.count.toString(),
            value.insertedSymbols,
            value.position.toString(),
            value.sequenceName ?: "",
        )
}
