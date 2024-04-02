package org.genspectrum.lapis.response

import org.genspectrum.lapis.controller.CsvRecord

data class NucleotideMutationResponse(
    val mutation: String,
    val count: Int,
    val proportion: Double,
    val sequenceName: String?,
    val mutationFrom: String,
    val mutationTo: String,
    val position: Int,
) : CsvRecord {
    override fun getValuesList() =
        listOf(
            mutation,
            count.toString(),
            proportion.toString(),
            sequenceName ?: "",
            mutationFrom,
            mutationTo,
            position.toString(),
        )

    override fun getHeader() =
        arrayOf(
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
    override fun getValuesList() =
        listOf(
            mutation,
            count.toString(),
            proportion.toString(),
            sequenceName,
            mutationFrom,
            mutationTo,
            position.toString(),
        )

    override fun getHeader() =
        arrayOf(
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
    override fun getValuesList() =
        listOf(
            insertion,
            count.toString(),
            insertedSymbols,
            position.toString(),
            sequenceName ?: "",
        )

    override fun getHeader() =
        arrayOf(
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
    override fun getValuesList() =
        listOf(
            insertion,
            count.toString(),
            insertedSymbols,
            position.toString(),
            sequenceName,
        )

    override fun getHeader() =
        arrayOf(
            "insertion",
            "count",
            "insertedSymbols",
            "position",
            "sequenceName",
        )
}
