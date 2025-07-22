package org.genspectrum.lapis.response

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.genspectrum.lapis.request.MutationsField
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

class MostRecentCommonAncestorCollection(
    override val records: Stream<DetailsData>,
    private val fields: List<String>,
) : RecordCollection<DetailsData> {
    override fun getHeader() = fields

    override fun mapToCsvValuesList(value: DetailsData): List<String?> =
        fields
            .map { value[it]?.toCsvValue() }
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
    private val fields: List<MutationsField>,
) : RecordCollection<MutationResponse> {
    override fun getHeader() = fields.map { it.value }

    override fun mapToCsvValuesList(value: MutationResponse): List<String?> =
        fields.map {
            when (it) {
                MutationsField.MUTATION -> value.mutation
                MutationsField.COUNT -> value.count.toString()
                MutationsField.COVERAGE -> value.coverage.toString()
                MutationsField.PROPORTION -> value.proportion.toString()
                MutationsField.SEQUENCE_NAME -> value.sequenceName?.value ?: ""
                MutationsField.MUTATION_FROM -> value.mutationFrom
                MutationsField.MUTATION_TO -> value.mutationTo
                MutationsField.POSITION -> value.position.toString()
            }
        }
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
