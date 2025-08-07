package org.genspectrum.lapis.controller

const val AGGREGATED_ROUTE = "/aggregated"
const val DETAILS_ROUTE = "/details"
const val NUCLEOTIDE_MUTATIONS_ROUTE = "/nucleotideMutations"
const val AMINO_ACID_MUTATIONS_ROUTE = "/aminoAcidMutations"
const val NUCLEOTIDE_INSERTIONS_ROUTE = "/nucleotideInsertions"
const val AMINO_ACID_INSERTIONS_ROUTE = "/aminoAcidInsertions"
const val ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE = "/alignedNucleotideSequences"
const val ALIGNED_AMINO_ACID_SEQUENCES_ROUTE = "/alignedAminoAcidSequences"
const val UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE = "/unalignedNucleotideSequences"
const val MOST_RECENT_COMMON_ANCESTOR_ROUTE = "/mostRecentCommonAncestor"
const val PHYLO_SUBTREE_ROUTE = "/phyloSubtree"

enum class ServeType {
    SEQUENCES,
    NEWICK,
    METADATA,
}

enum class SampleRoute(
    val pathSegment: String,
    val serveType: ServeType,
) {
    AGGREGATED(AGGREGATED_ROUTE),
    DETAILS(DETAILS_ROUTE),
    MOST_RECENT_COMMON_ANCESTOR(MOST_RECENT_COMMON_ANCESTOR_ROUTE),
    NUCLEOTIDE_MUTATIONS(NUCLEOTIDE_MUTATIONS_ROUTE),
    AMINO_ACID_MUTATIONS(AMINO_ACID_MUTATIONS_ROUTE),
    NUCLEOTIDE_INSERTIONS(NUCLEOTIDE_INSERTIONS_ROUTE),
    AMINO_ACID_INSERTIONS(AMINO_ACID_INSERTIONS_ROUTE),
    ALIGNED_NUCLEOTIDE_SEQUENCES(ALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE, serveType = ServeType.SEQUENCES),
    ALIGNED_AMINO_ACID_SEQUENCES(ALIGNED_AMINO_ACID_SEQUENCES_ROUTE, serveType = ServeType.SEQUENCES),
    UNALIGNED_NUCLEOTIDE_SEQUENCES(UNALIGNED_NUCLEOTIDE_SEQUENCES_ROUTE, serveType = ServeType.SEQUENCES),
    PHYLO_SUBTREE(PHYLO_SUBTREE_ROUTE, serveType = ServeType.NEWICK);

    constructor(pathSegment: String)
            : this(pathSegment, ServeType.METADATA)
}
