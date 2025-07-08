package org.genspectrum.lapis.controller

import org.genspectrum.lapis.controller.LapisMediaType.TEXT_CSV_WITHOUT_HEADERS_VALUE

const val DETAILS_ENDPOINT_DESCRIPTION = """Returns the specified metadata fields of sequences matching the filter."""
const val AGGREGATED_ENDPOINT_DESCRIPTION =
    """Returns the number of sequences matching the specified sequence filters."""
const val NUCLEOTIDE_MUTATION_ENDPOINT_DESCRIPTION =
    """Returns the number of sequences matching the specified sequence filters, grouped by nucleotide mutations.
    Additionally, the relative frequency of each mutation is returned. It is relative to the total number of sequences 
    matching the specified sequence filters with non-ambiguous reads at that position."""
const val AMINO_ACID_MUTATIONS_ENDPOINT_DESCRIPTION =
    """Returns the number of sequences matching the specified sequence filters, grouped by amino acid mutations. 
    Additionally, the relative frequency of each mutation is returned. It is relative to the total number of sequences
    matching the specified sequence filters with non-ambiguous reads at that position."""
const val NUCLEOTIDE_INSERTIONS_ENDPOINT_DESCRIPTION =
    """Returns the number of sequences matching the specified sequence filters, grouped by nucleotide insertions."""
const val AMINO_ACID_INSERTIONS_ENDPOINT_DESCRIPTION =
    """Returns a list of mutations along with the counts and proportions whose proportions are greater 
    than or equal to the specified minProportion. Only sequences matching the specified sequence filters are
    considered."""
const val INFO_ENDPOINT_DESCRIPTION = "Returns information about LAPIS."
const val DATABASE_CONFIG_ENDPOINT_DESCRIPTION = "Returns the database configuration."
const val LINEAGE_DEFINITION_ENDPOINT_DESCRIPTION = """Download the lineage definition file used for a certain column.
This can be used to reconstruct the lineage tree. 
"""
const val REFERENCE_GENOME_ENDPOINT_DESCRIPTION = "Returns the reference genome."
const val ALIGNED_AMINO_ACID_SEQUENCE_ENDPOINT_DESCRIPTION =
    """Returns a string of aligned amino acid sequences. Only sequences matching the specified
    sequence filters are considered."""
const val ALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """Returns a string of aligned nucleotide sequences. Only sequences matching the 
    specified sequence filters are considered."""
const val UNALIGNED_SINGLE_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """Returns a string of unaligned nucleotide sequences. Only sequences matching the 
    specified sequence filters are considered."""
const val ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """Returns a string of aligned nucleotide sequences of the requested segment.
    Only sequences matching the specified sequence filters are considered."""
const val ALL_ALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """"Returns the aligned nucleotide sequences of all requested segments that match the given filter criteria."""
const val UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """Returns a string of unaligned nucleotide sequences of the requested segment.
    Only sequences matching the specified sequence filters are considered.
    Sequences that don't have an unaligned sequence for this segment are omitted."""
const val ALL_UNALIGNED_MULTI_SEGMENTED_NUCLEOTIDE_SEQUENCE_ENDPOINT_DESCRIPTION =
    """"Returns the unaligned nucleotide sequences of all requested segments that match the given filter criteria."""

const val AGGREGATED_GROUP_BY_FIELDS_DESCRIPTION =
    """The fields to stratify by.
If empty, only the overall count is returned.
If requesting CSV or TSV data, the columns are ordered in the same order as the fields are specified here."""
const val AGGREGATED_ORDER_BY_FIELDS_DESCRIPTION =
    """The fields of the response to order by. 
    Fields specified here must either be \"count\" or also be present in \"fields\"."""
const val DETAILS_FIELDS_DESCRIPTION =
    """The fields that the response items should contain.
If empty, all fields are returned.
If requesting CSV or TSV data, the columns are ordered in the same order as the fields are specified here.
"""
const val MUTATIONS_FIELDS_DESCRIPTION =
    """The fields that the response items should contain.
If empty, all fields are returned.
If requesting CSV or TSV data, the columns are ordered in the same order as the fields are specified here.
"""
const val DETAILS_ORDER_BY_FIELDS_DESCRIPTION =
    """The fields of the response to order by. Fields specified here must also be present in \"fields\"."""
const val LIMIT_DESCRIPTION = """The maximum number of entries to return in the response"""
const val OFFSET_DESCRIPTION =
    """The offset of the first entry to return in the response. 
    This is useful for pagination in combination with \"limit\"."""
const val DATA_FORMAT_DESCRIPTION = """The data format of the response.
Alternatively, the data format can be specified by setting the \"Accept\"-header.
You can include the parameter to return the CSV/TSV without headers: "$TEXT_CSV_WITHOUT_HEADERS_VALUE". 
When both are specified, the request parameter takes precedence over the header."""

const val SEQUENCES_DATA_FORMAT_DESCRIPTION = """The format of the sequences in the response.
Alternatively, the data format can be specified by setting the \"Accept\"-header.
When both are specified, the request parameter takes precedence over the header.
"""

const val NUCLEOTIDE_FASTA_HEADER_TEMPLATE_DESCRIPTION = """Use this to customize the header of the FASTA file.
This parameter will be ignored if the data format is not FASTA.
It supports free-form text, placeholders of the form "{<metadataField>}" to include metadata fields in the header and
"{.segment}" to include the segment name in the header.
"""

const val AMINO_ACID_FASTA_HEADER_TEMPLATE_DESCRIPTION = """Use this to customize the header of the FASTA file.
This parameter will be ignored if the data format is not FASTA.
It supports free-form text, placeholders of the form "{<metadataField>}" to include metadata fields in the header and
"{.gene}" to include the gene name in the header.
"""

private const val MAYBE_DESCRIPTION = """
A mutation can be wrapped in a maybe expression "MAYBE(\<mutation\>)"
to include sequences with ambiguous symbols at the given position.
"""

const val NUCLEOTIDE_MUTATION_DESCRIPTION = """
A nucleotide mutation in the format "\<sequenceName\>?:\<fromSymbol\>?\<position\>\<toSymbol\>?".  
If the sequenceName is not provided, LAPIS will use the default sequence name. 
The fromSymbol is optional. 
If the toSymbol is not provided, the statement means "has any mutation at the given position".
$MAYBE_DESCRIPTION
"""

const val AMINO_ACID_MUTATION_DESCRIPTION = """
A amino acid mutation in the format "\<gene\>:\<position\>\<toSymbol\>?".  
If the toSymbol is not provided, the statement means "has any mutation at the given position". 
$MAYBE_DESCRIPTION
"""
