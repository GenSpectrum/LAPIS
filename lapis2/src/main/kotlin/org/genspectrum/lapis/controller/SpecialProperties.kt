package org.genspectrum.lapis.controller

const val FORMAT_PROPERTY = "dataFormat"
const val ACCESS_KEY_PROPERTY = "accessKey"
const val MIN_PROPORTION_PROPERTY = "minProportion"
const val FIELDS_PROPERTY = "fields"
const val NUCLEOTIDE_MUTATIONS_PROPERTY = "nucleotideMutations"
const val AMINO_ACID_MUTATIONS_PROPERTY = "aminoAcidMutations"
const val ORDER_BY_PROPERTY = "orderBy"
const val LIMIT_PROPERTY = "limit"
const val OFFSET_PROPERTY = "offset"

val SPECIAL_REQUEST_PROPERTIES = listOf(
    MIN_PROPORTION_PROPERTY,
    ACCESS_KEY_PROPERTY,
    FIELDS_PROPERTY,
    NUCLEOTIDE_MUTATIONS_PROPERTY,
    AMINO_ACID_MUTATIONS_PROPERTY,
    ORDER_BY_PROPERTY,
    LIMIT_PROPERTY,
    OFFSET_PROPERTY,
    FORMAT_PROPERTY,
)
