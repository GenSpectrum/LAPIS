package org.genspectrum.lapis.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.CsvRecord

data class NucleotideMutationResponse(
    @Schema(
        example = "main:G29741T",
        description = "If the genome only contains one segment then this is: " +
            "(mutationFrom)(position)(mutationTo)." +
            "If it has more than one segment (e.g., influenza), then the sequence is contained here: " +
            "(sequenceName):(mutationFrom)(position)" +
            "(mutationTo)",
    ) val mutation: String,
    @Schema(
        example = "42",
        description = "Total number of sequences with this mutation matching the given sequence filter criteria",
    ) val count: Int,
    @Schema(
        example = "0.54321",
        description = "Number of sequences with this mutation divided by the total number sequences matching the " +
            "given filter criteria with non-ambiguous reads at that position",
    ) val proportion: Double,
    @Schema(
        example = "main",
        description = "The name of the segment in which the mutation occurs. Null if the genome is single-segmented.",
    )
    val sequenceName: String?,
    @Schema(
        example = "G",
        description = "The nucleotide symbol in the reference genome at the position of the mutation",
    )
    val mutationFrom: String,
    @Schema(
        example = "T",
        description = "The nucleotide symbol that the mutation changes to",
    )
    val mutationTo: String,
    @Schema(
        example = "29741",
        description = "The position in the reference genome where the mutation occurs",
    )
    val position: Int,
) : CsvRecord {
    override fun asArray() = arrayOf(mutation, count.toString(), proportion.toString())

    @JsonIgnore
    override fun getHeader() = arrayOf("mutation", "count", "proportion")
}

data class AminoAcidMutationResponse(
    @Schema(
        example = "ORF1a:G29741T",
        description = "Of the format (sequenceName):(mutationFrom)(position)(mutationTo)",
    ) val mutation: String,
    @Schema(
        example = "42",
        description = "Total number of sequences with this mutation matching the given sequence filter criteria",
    ) val count: Int,
    @Schema(
        example = "0.54321",
        description = "Number of sequences with this mutation divided by the total number sequences matching the " +
            "given filter criteria with non-ambiguous reads at that position",
    ) val proportion: Double,
    @Schema(
        example = "ORF1a",
        description = "The name of the gene in which the mutation occurs.",
    )
    val sequenceName: String,
    @Schema(
        example = "G",
        description = "The amino acid symbol in the reference genome at the position of the mutation",
    )
    val mutationFrom: String,
    @Schema(
        example = "T",
        description = "The amino acid symbol that the mutation changes to",
    )
    val mutationTo: String,
    @Schema(
        example = "29741",
        description = "The position in the reference genome where the mutation occurs",
    )
    val position: Int,
) : CsvRecord {
    override fun asArray() = arrayOf(mutation, count.toString(), proportion.toString())

    @JsonIgnore
    override fun getHeader() = arrayOf("mutation", "count", "proportion")
}

data class NucleotideInsertionResponse(
    @Schema(
        example = "ins_22204:CAGAAG",
        description =
        "|A nucleotide insertion in the format \"ins_\\<segment\\>?:\\<position\\>:\\<insertedSymbols\\>\".  " +
            "|If the pathogen has only one segment LAPIS will omit the segment name.",
    )
    val insertion: String,
    @Schema(
        example = "123",
        description = "Total number of sequences with this insertion matching the given sequence filter criteria",
    )
    val count: Int,
    @Schema(
        example = "CAGAAG",
        description = "The nucleotide symbols that were inserted at the given position",
    )
    val insertedSymbols: String,
    @Schema(
        example = "22204",
        description = "The position in the reference genome where the insertion occurs",
    )
    val position: Int,
    @Schema(
        example = "main",
        description = "The name of the segment in which the insertion occurs. Null if the genome is single-segmented.",
    )
    val sequenceName: String?,
) : CsvRecord {
    override fun asArray() = arrayOf(insertion, count.toString())

    @JsonIgnore
    override fun getHeader() = arrayOf("insertion", "count")
}

data class AminoAcidInsertionResponse(
    @Schema(
        example = "ins_ORF1a:22204:CAGAAG",
        description =
        "|A amino acid insertion in the format \"ins_\\<gene\\>:\\<position\\>:\\<insertion\\>\".",
    )
    val insertion: String,
    @Schema(
        example = "123",
        description = "Total number of sequences with this insertion matching the given sequence filter criteria",
    )
    val count: Int,
    @Schema(
        example = "CAGAAG",
        description = "The amino acid symbols that were inserted at the given position",
    )
    val insertedSymbols: String,
    @Schema(
        example = "22204",
        description = "The position in the reference genome where the insertion occurs",
    )
    val position: Int,
    @Schema(
        example = "ORF1a",
        description = "The name of the gene in which the insertion occurs.",
    )
    val sequenceName: String,
) : CsvRecord {
    override fun asArray() = arrayOf(insertion, count.toString())

    @JsonIgnore
    override fun getHeader() = arrayOf("insertion", "count")
}
