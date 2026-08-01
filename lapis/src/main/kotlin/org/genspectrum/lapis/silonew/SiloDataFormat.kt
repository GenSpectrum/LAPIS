package org.genspectrum.lapis.silonew

import java.util.Locale

/** A textual representation of a SILO query result. */
sealed interface SiloDataFormat {
    /** A JSON array containing one object per result row. */
    data object Json : SiloDataFormat

    /** One JSON object per line. */
    data object Ndjson : SiloDataFormat

    /** Comma-separated records using CSV quoting rules. */
    data class Csv(
        val includeHeader: Boolean = true,
    ) : SiloDataFormat

    /**
     * Tab-separated records.
     *
     * With [escapeSpecialCharacters], tabs and newlines within values are written as `\t` and `\n`. Otherwise,
     * standard CSV quoting rules are applied with a tab delimiter.
     */
    data class Tsv(
        val includeHeader: Boolean = true,
        val escapeSpecialCharacters: Boolean = false,
    ) : SiloDataFormat

    /**
     * FASTA records built from [sequenceFields] and a LAPIS-style [headerTemplate].
     *
     * Template placeholders refer to result fields. `{.segment}` and `{.gene}` resolve to the current sequence field
     * name. One record is emitted for each non-null sequence field in every row.
     */
    class Fasta(
        sequenceFields: List<SiloField>,
        val headerTemplate: String,
    ) : SiloDataFormat {
        val sequenceFields: List<SiloField> = sequenceFields.toList()

        init {
            require(this.sequenceFields.isNotEmpty()) { "FASTA output requires at least one sequence field" }
            val uniqueFieldNames = this.sequenceFields.distinctBy { it.name.lowercase(Locale.ROOT) }
            require(uniqueFieldNames.size == this.sequenceFields.size) {
                "FASTA sequence fields must be unique"
            }
        }
    }

    /** The single non-null value of [field], written as Newick text. */
    data class Newick(
        val field: SiloField,
    ) : SiloDataFormat
}

internal val SiloDataFormat.contentType: String
    get() = when (this) {
        SiloDataFormat.Json -> "application/json"
        SiloDataFormat.Ndjson -> "application/x-ndjson;charset=UTF-8"
        is SiloDataFormat.Csv -> if (includeHeader) "text/csv;charset=UTF-8" else "text/plain"
        is SiloDataFormat.Tsv -> if (includeHeader) "text/tab-separated-values;charset=UTF-8" else "text/plain"
        is SiloDataFormat.Fasta -> "text/x-fasta;charset=UTF-8"
        is SiloDataFormat.Newick -> "text/x-nh;charset=UTF-8"
    }
