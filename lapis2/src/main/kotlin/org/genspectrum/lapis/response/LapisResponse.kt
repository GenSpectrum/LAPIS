package org.genspectrum.lapis.response

import io.swagger.v3.oas.annotations.media.Schema
import org.genspectrum.lapis.controller.CsvRecord

data class NucleotideMutationResponse(
    @Schema(
        example = "sequenceName:G29741T",
        description = "If the genome only contains one segment then this is: " +
            "(nucleotide symbol in reference genome)(position in genome)(mutation's target nucleotide symbol)." +
            "If it has more than one segment (e.g., influenza), then the sequence is contained here: " +
            "(sequenceName):(nucleotide symbol in reference genome)(position in genome)" +
            "(mutation's target nucleotide symbol)",
    ) val mutation: String,
    @Schema(
        example = "42",
        description = "Total number of sequences with this mutation matching the given sequence filter criteria",
    ) val count: Int,
    @Schema(
        example = "0.54321",
        description = "Number of sequences with this mutation divided by the total number sequences matching the " +
            "given filter criteria",
    ) val proportion: Double,
) : CsvRecord {
    override fun asArray() = arrayOf(mutation, count.toString(), proportion.toString())
    override fun getHeader() = arrayOf("mutation", "count", "proportion")
}

data class AminoAcidMutationResponse(
    @Schema(
        example = "ORF1a:G29741T",
        description = "(Gene):(amino acid symbol in reference genome)(position in genome)" +
            "(mutation's target nucleotide symbol)",
    ) val mutation: String,
    @Schema(
        example = "42",
        description = "Total number of sequences with this mutation matching the given sequence filter criteria",
    ) val count: Int,
    @Schema(
        example = "0.54321",
        description = "Number of sequences with this mutation divided by the total number sequences matching the " +
            "given filter criteria",
    ) val proportion: Double,
) : CsvRecord {
    override fun asArray() = arrayOf(mutation, count.toString(), proportion.toString())
    override fun getHeader() = arrayOf("mutation", "count", "proportion")
}

data class NucleotideInsertionResponse(
    @Schema(
        example = "ins_22204:CAGAA",
        description =
        "|A nucleotide insertion in the format \"ins_\\<segment\\>?:\\<position\\>:\\<insertion\\>\".  " +
            "|If the pathogen has only one segment LAPIS will omit the segment name.",
    )
    val insertion: String,
    @Schema(
        example = "123",
        description = "Total number of sequences with this insertion matching the given sequence filter criteria",
    )
    val count: Int,
) : CsvRecord {
    override fun asArray() = arrayOf(insertion, count.toString())
    override fun getHeader() = arrayOf("insertion", "count")
}

data class AminoAcidInsertionResponse(
    @Schema(
        example = "ins_ORF1a:22204:CAGAA",
        description =
        "|A amino acid insertion in the format \"ins_\\<gene\\>:\\<position\\>:\\<insertion\\>\".",
    )
    val insertion: String,
    @Schema(
        example = "123",
        description = "Total number of sequences with this insertion matching the given sequence filter criteria",
    )
    val count: Int,
) : CsvRecord {
    override fun asArray() = arrayOf(insertion, count.toString())
    override fun getHeader() = arrayOf("insertion", "count")
}
