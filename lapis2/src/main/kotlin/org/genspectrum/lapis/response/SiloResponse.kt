package org.genspectrum.lapis.response

import io.swagger.v3.oas.annotations.media.Schema

data class AggregatedResponse(val count: Int)

data class MutationData(
    @Schema(
        example = "G29741T",
        description = "(nucleotide symbol in reference genome)(position in genome)" +
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
)
