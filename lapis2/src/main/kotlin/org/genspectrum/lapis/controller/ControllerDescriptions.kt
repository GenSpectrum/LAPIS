package org.genspectrum.lapis.controller

const val DETAILS_ENDPOINT_DESCRIPTION = "Returns the specified metadata fields of sequences matching the filter."
const val AGGREGATED_ENDPOINT_DESCRIPTION = "Returns the number of sequences matching the specified sequence filters"
const val NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by nucleotide mutations."
const val AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by amino acid mutations."
const val NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION =
    "Returns the number of sequences matching the specified sequence filters, " +
        "grouped by nucleotide insertions."
const val AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION =
    "Returns a list of mutations along with the counts and proportions whose proportions are greater " +
        "than or equal to the specified minProportion. Only sequences matching the specified " +
        "sequence filters are considered."
const val INFO_ENDPOINT_DESCRIPTION = "Returns information about LAPIS"

const val AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION =
    "Returns a string of fasta formated amino acid sequences. Only sequences matching the specified " +
        "sequence filters are considered."
const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    "The fields to stratify by. If empty, only the overall count is returned"
const val AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by. " +
        "Fields specified here must either be \"count\" or also be present in \"fields\"."
const val DETAILS_FIELDS_DESCRIPTION =
    "The fields that the response items should contain. If empty, all fields are returned"
const val DETAILS_ORDER_BY_FIELDS_DESCRIPTION =
    "The fields of the response to order by. Fields specified here must also be present in \"fields\"."
const val LIMIT_DESCRIPTION = "The maximum number of entries to return in the response"
const val OFFSET_DESCRIPTION = "The offset of the first entry to return in the response. " +
    "This is useful for pagination in combination with \"limit\"."
const val FORMAT_DESCRIPTION = "The data format of the response. " +
    "Alternatively, the data format can be specified by setting the \"Accept\"-header. When both are specified, " +
    "this parameter takes precedence."

const val ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    "Returns a string of fasta formatted aligned nucleotide sequences. Only sequences matching the " +
        "specified sequence filters are considered."
const val ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    "Returns a string of fasta formatted aligned nucleotide sequences of the requested segment. " +
        "Only sequences matching the specified sequence filters are considered."
