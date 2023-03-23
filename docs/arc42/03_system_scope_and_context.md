# System Scope and Context

This chapter specifies the boundaries of SILO-LAPIS and describes the interfaces to other systems and users.

![systemScopeAndContextBusiness](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/GenSpectrum/LAPIS/master/docs/arc42/images/systemScopeAndContext.puml)

**LAPIS**
- is provisioned by the maintainer, who provides a configuration file
- can be accessed by the end-user through a REST interface with endpoints
  - aggregated data
    - returns how many sequences fulfill the provided filter criteria 
  - aa-mutations, nuc-mutations
    - returns a list of mutations (with proportion, count), which fulfill the provided filter criteria
    - the proportion is relative to all sequences, which fulfill the provided criteria 
  - details
    - returns a list of metadata, for sequences which fulfill the provided criteria
  - aa-sequence, nuc-sequence, nuc-sequence-aligned
    - returns the corresponding sequences, which fulfill the provided criteria
  - the provided criteria can be a compilation of
    - metadata
    - specific mutations
    - pango lineage
- the maintainer can trigger an update upon which LAPIS reads data from the disc and stores it internally 



**Sequence data on disk**

Preprocessing:
- original data most likely provided by Genbank
- original data must be preprocessed (for example by Nextstrain), which includes the following steps 
    - align nucleotide data
    - assign amino acids
    - determine insertions

Required files:

nucleotide_sequences.fasta
- list of nucleotide sequences in the [fasta format](https://en.wikipedia.org/wiki/FASTA_format)

nucleotide_sequences_aligned.fasta
- same as nucleotide_sequences.fasta but aligned to reference genome

aminoacid_sequences.fasta (multiple)
- list of AA sequences in the fasta format
- aligned
- one file for each gene

metadata_and_quality_control.tsv
- list of metadata and quality control

insertions.tsv
- list of nucleotide insertions and AA insertions for each sequence

alias.tsv
- list of pangolin aliases

**Configuration file**
includes:
- build info for LAPIS
  - database fields with primary key
  - access keys (for restricted data access)
- reference genome
  - nucleotide sequence
  - AA sequence with start and end position in nucleotide sequence