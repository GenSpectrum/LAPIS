package org.genspectrum.lapis.response

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_EXAMPLE
import org.genspectrum.lapis.openApi.LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION
import org.genspectrum.lapis.openApi.LAPIS_INFO_DESCRIPTION
import org.genspectrum.lapis.openApi.REQUEST_ID_HEADER_DESCRIPTION
import org.genspectrum.lapis.openApi.REQUEST_INFO_STRING_DESCRIPTION
import org.springframework.http.ProblemDetail

data class LapisResponse<Data>(val data: Data, val info: LapisInfo = LapisInfo())

data class LapisErrorResponse(val error: ProblemDetail, val info: LapisInfo = LapisInfo())

private const val REPORT_TO =
    "Please report to https://github.com/GenSpectrum/LAPIS/issues in case you encounter any unexpected issues. " +
        "Please include the request ID and the requestInfo in your report."

@Schema(description = LAPIS_INFO_DESCRIPTION)
data class LapisInfo(
    @Schema(
        description = LAPIS_DATA_VERSION_RESPONSE_DESCRIPTION,
        example = LAPIS_DATA_VERSION_EXAMPLE,
    )
    var dataVersion: String? = null,
    @Schema(
        description = REQUEST_ID_HEADER_DESCRIPTION,
        example = "dfb342ea-3607-4caf-b35e-9aba75d06f81",
    )
    var requestId: String? = null,
    @Schema(
        description = REQUEST_INFO_STRING_DESCRIPTION,
        example = "my_instance on my.server.com at 2024-01-01T12:00:00.0000",
    )
    var requestInfo: String? = null,
    @Schema(example = REPORT_TO)
    val reportTo: String = REPORT_TO,
)

data class NucleotideMutationResponse(
    val mutation: String,
    val count: Int,
    val proportion: Double,
    val sequenceName: String?,
    val mutationFrom: String,
    val mutationTo: String,
    val position: Int,
) : CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        listOf(
            mutation,
            count.toString(),
            proportion.toString(),
            sequenceName ?: "",
            mutationFrom,
            mutationTo,
            position.toString(),
        )

    override fun getHeader(comparator: Comparator<String>) =
        listOf(
            "mutation",
            "count",
            "proportion",
            "sequenceName",
            "mutationFrom",
            "mutationTo",
            "position",
        )
}

data class AminoAcidMutationResponse(
    val mutation: String,
    val count: Int,
    val proportion: Double,
    val sequenceName: String,
    val mutationFrom: String,
    val mutationTo: String,
    val position: Int,
) : CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        listOf(
            mutation,
            count.toString(),
            proportion.toString(),
            sequenceName,
            mutationFrom,
            mutationTo,
            position.toString(),
        )

    override fun getHeader(comparator: Comparator<String>) =
        listOf(
            "mutation",
            "count",
            "proportion",
            "sequenceName",
            "mutationFrom",
            "mutationTo",
            "position",
        )
}

data class NucleotideInsertionResponse(
    val insertion: String,
    val count: Int,
    val insertedSymbols: String,
    val position: Int,
    val sequenceName: String?,
) : CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        listOf(
            insertion,
            count.toString(),
            insertedSymbols,
            position.toString(),
            sequenceName ?: "",
        )

    override fun getHeader(comparator: Comparator<String>) =
        listOf(
            "insertion",
            "count",
            "insertedSymbols",
            "position",
            "sequenceName",
        )
}

data class AminoAcidInsertionResponse(
    val insertion: String,
    val count: Int,
    val insertedSymbols: String,
    val position: Int,
    val sequenceName: String,
) : CsvRecord {
    override fun getValuesList(comparator: Comparator<String>) =
        listOf(
            insertion,
            count.toString(),
            insertedSymbols,
            position.toString(),
            sequenceName,
        )

    override fun getHeader(comparator: Comparator<String>) =
        listOf(
            "insertion",
            "count",
            "insertedSymbols",
            "position",
            "sequenceName",
        )
}
