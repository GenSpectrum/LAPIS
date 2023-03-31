package org.genspectrum.lapis.model

import org.genspectrum.lapis.response.AggregatedResponse
import org.genspectrum.lapis.silo.And
import org.genspectrum.lapis.silo.NucleotideSymbolEquals
import org.genspectrum.lapis.silo.PangoLineageEquals
import org.genspectrum.lapis.silo.SiloAction
import org.genspectrum.lapis.silo.SiloClient
import org.genspectrum.lapis.silo.SiloFilterExpression
import org.genspectrum.lapis.silo.SiloQuery
import org.genspectrum.lapis.silo.StringEquals
import org.genspectrum.lapis.silo.True
import org.springframework.stereotype.Component

@Component
class AggregatedModel(private val siloClient: SiloClient) {
    fun handleRequest(filterParameter: Map<String, String>): AggregatedResponse {
        if (filterParameter.isEmpty()) {
            return siloClient.sendQuery(
                SiloQuery(SiloAction.aggregated(), True),
            )
        }

        return siloClient.sendQuery(
            SiloQuery(
                SiloAction.aggregated(),
                And(
                    filterParameter.map { mapToSiloFilter(it.key, it.value) },
                ),
            ),
        )
    }

    fun mapToSiloFilter(key: String, value: String) = when (key) {
        "pangoLineage" -> mapToPangoLineageFilter(value)
        "nucleotideMutations" -> mapToNucleotideFilter(value)
        else -> StringEquals(key, value)
    }

    private fun mapToPangoLineageFilter(value: String) = when {
        value.endsWith(".*") -> PangoLineageEquals(value.substringBeforeLast(".*"), includeSublineages = true)
        value.endsWith('*') -> PangoLineageEquals(value.substringBeforeLast('*'), includeSublineages = true)
        value.endsWith('.') -> throw IllegalArgumentException(
            "Invalid pango lineage: $value must not end with a dot. Did you mean '$value*'?",
        )
        else -> PangoLineageEquals(value, includeSublineages = false)
    }

    private fun mapToNucleotideFilter(userInput: String): SiloFilterExpression {
        val mutations = userInput.split(",")

        return And(
            mutations.map { mutationExpression ->
                val match = NUCLEOTIDE_MUTATION_REGEX.find(mutationExpression)
                    ?: throw IllegalArgumentException("Invalid nucleotide mutation: $mutationExpression")

                val (_, position: String, symbolTo: String) = match.destructured

                NucleotideSymbolEquals(position.toInt(), symbolTo)
            },
        )
    }

    companion object {
        private var NUCLEOTIDE_MUTATION_REGEX: Regex

        init {
            val validNucleotideSymbols = listOf("A", "C", "G", "T")
            val validMutationNucleotideSymbols =
                listOf("N", "-", "M", "R", "W", "S", "Y", "K", "V", "H", "D", "B", ".") +
                    validNucleotideSymbols
            val validSymbolsFrom = validNucleotideSymbols.joinToString { it }
            var validSymbolsTo = validMutationNucleotideSymbols.joinToString { it }
            validSymbolsTo = validSymbolsTo.replace(".", "\\.")
            validSymbolsTo = validSymbolsTo.replace("-", "\\-")

            NUCLEOTIDE_MUTATION_REGEX =
                Regex(
                    "(?<symbolFrom>^[$validSymbolsFrom]?)(?<position>\\d+)(?<symbolTo>[$validSymbolsTo]\$)",
                )
        }
    }
}
