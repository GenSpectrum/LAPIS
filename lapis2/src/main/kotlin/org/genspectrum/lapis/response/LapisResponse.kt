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
