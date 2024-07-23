package org.genspectrum.lapis.request

import com.fasterxml.jackson.databind.node.JsonNodeType

const val FORMAT_PROPERTY = "dataFormat"
const val ACCESS_KEY_PROPERTY = "accessKey"
const val MIN_PROPORTION_PROPERTY = "minProportion"
const val FIELDS_PROPERTY = "fields"
const val NUCLEOTIDE_MUTATIONS_PROPERTY = "nucleotideMutations"
const val AMINO_ACID_MUTATIONS_PROPERTY = "aminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_PROPERTY = "nucleotideInsertions"
const val AMINO_ACID_INSERTIONS_PROPERTY = "aminoAcidInsertions"
const val ORDER_BY_PROPERTY = "orderBy"
const val LIMIT_PROPERTY = "limit"
const val OFFSET_PROPERTY = "offset"
const val DOWNLOAD_AS_FILE_PROPERTY = "downloadAsFile"
const val DOWNLOAD_FILE_BASENAME_PROPERTY = "downloadFileBasename"
const val COMPRESSION_PROPERTY = "compression"

val SPECIAL_REQUEST_PROPERTY_TYPES = mapOf(
    FORMAT_PROPERTY to JsonNodeType.STRING,
    ACCESS_KEY_PROPERTY to JsonNodeType.STRING,
    MIN_PROPORTION_PROPERTY to JsonNodeType.NUMBER,
    FIELDS_PROPERTY to JsonNodeType.ARRAY,
    NUCLEOTIDE_MUTATIONS_PROPERTY to JsonNodeType.ARRAY,
    AMINO_ACID_MUTATIONS_PROPERTY to JsonNodeType.ARRAY,
    NUCLEOTIDE_INSERTIONS_PROPERTY to JsonNodeType.ARRAY,
    AMINO_ACID_INSERTIONS_PROPERTY to JsonNodeType.ARRAY,
    ORDER_BY_PROPERTY to JsonNodeType.ARRAY,
    LIMIT_PROPERTY to JsonNodeType.NUMBER,
    OFFSET_PROPERTY to JsonNodeType.NUMBER,
    DOWNLOAD_AS_FILE_PROPERTY to JsonNodeType.BOOLEAN,
    DOWNLOAD_FILE_BASENAME_PROPERTY to JsonNodeType.STRING,
    COMPRESSION_PROPERTY to JsonNodeType.STRING,
)

val SPECIAL_REQUEST_PROPERTIES = SPECIAL_REQUEST_PROPERTY_TYPES.keys
