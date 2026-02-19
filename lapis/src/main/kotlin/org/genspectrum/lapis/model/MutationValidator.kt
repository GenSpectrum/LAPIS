package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException

val deletionSymbols = setOf('-')
val querySymbols = setOf('.', '?')
val nucleotideSymbols = setOf('A', 'C', 'G', 'T')
val ambiguousNucleotideSymbols = setOf('M', 'R', 'W', 'S', 'Y', 'K', 'V', 'H', 'D', 'B', 'N')
val allNucleotideQuerySymbols = nucleotideSymbols + ambiguousNucleotideSymbols + deletionSymbols + querySymbols
val aaSymbols = setOf(
    'A',
    'R',
    'N',
    'D',
    'C',
    'E',
    'Q',
    'G',
    'H',
    'I',
    'L',
    'K',
    'M',
    'F',
    'P',
    'S',
    'T',
    'W',
    'Y',
    'V',
    '*',
)
val ambiguousAaSymbols = setOf('B', 'Z', 'X')
val allAaQuerySymbols = aaSymbols + ambiguousAaSymbols + deletionSymbols + querySymbols

fun validateNucleotideSymbol(c: Char) {
    if (c.uppercaseChar() !in allNucleotideQuerySymbols) {
        throw BadRequestException("Invalid nucleotide symbol: $c")
    }
}

fun validateAminoAcidSymbol(c: Char) {
    if (c.uppercaseChar() !in allAaQuerySymbols) {
        throw BadRequestException("Invalid amino acid symbol: $c")
    }
}
