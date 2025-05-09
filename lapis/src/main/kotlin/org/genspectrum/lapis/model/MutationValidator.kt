package org.genspectrum.lapis.model

import org.genspectrum.lapis.controller.BadRequestException

val nucleotideSymbols = setOf('A', 'C', 'G', 'T')
val ambiguousNucSymbols = setOf('M', 'R', 'W', 'S', 'Y', 'K', 'V', 'H', 'D', 'B', 'N', '-', '.', '?')
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
val ambiguousAaSymbols = setOf('X', '-', '.', '?')

fun validateNucleotideSymbol(c: Char) {
    if (c.uppercaseChar() !in ambiguousNucSymbols && c.uppercaseChar() !in nucleotideSymbols) {
        throw BadRequestException("Invalid nucleotide symbol: $c")
    }
}

fun validateAminoAcidSymbol(c: Char) {
    if (c.uppercaseChar() !in ambiguousAaSymbols && c.uppercaseChar() !in aaSymbols) {
        throw BadRequestException("Invalid amino acid symbol: $c")
    }
}
