-- Basic information


create table data_version
(
  dataset text primary key,
  timestamp bigint not null
);


-- Source: mpox from Nextstrain

create table y_nextstrain_mpox
(
  metadata_hash text,
  seq_original_hash text,
  seq_aligned_hash text,
  accession text primary key,
  accession_rev text,
  strain text,
  sra_accession text,
  date date,
  year integer,
  month integer,
  day integer,
  date_original text,
  date_submitted date,
  region text,
  country text,
  division text,
  location text,
  host text,
  authors text,
  institution text,
  seq_original_compressed bytea,
  seq_aligned_compressed bytea,
  aa_seqs_compressed bytea,
  aa_mutations text,
  aa_unknowns text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text,
  nuc_unknowns text,

  -- Nextclade stuff
  clade text,
  nextclade_total_substitutions int,
  nextclade_total_deletions int,
  nextclade_total_insertions int,
  nextclade_total_frame_shifts int,
  nextclade_total_aminoacid_substitutions int,
  nextclade_total_aminoacid_deletions int,
  nextclade_total_aminoacid_insertions int,
  nextclade_total_missing int,
  nextclade_total_non_acgtns int,
  nextclade_total_pcr_primer_changes int,
  nextclade_pcr_primer_changes text,
  nextclade_alignment_score float,
  nextclade_alignment_start int,
  nextclade_alignment_end int,
  nextclade_qc_overall_score float,
  nextclade_qc_overall_status text,
  nextclade_qc_missing_data_missing_data_threshold float,
  nextclade_qc_missing_data_score float,
  nextclade_qc_missing_data_status text,
  nextclade_qc_missing_data_total_missing int,
  nextclade_qc_mixed_sites_mixed_sites_threshold float,
  nextclade_qc_mixed_sites_score float,
  nextclade_qc_mixed_sites_status text,
  nextclade_qc_mixed_sites_total_mixed_sites int,
  nextclade_qc_private_mutations_cutoff float,
  nextclade_qc_private_mutations_excess float,
  nextclade_qc_private_mutations_score float,
  nextclade_qc_private_mutations_status text,
  nextclade_qc_private_mutations_total int,
  nextclade_qc_snp_clusters_clustered_snps text,
  nextclade_qc_snp_clusters_score float,
  nextclade_qc_snp_clusters_status text,
  nextclade_qc_snp_clusters_total_snps int,
  nextclade_qc_frame_shifts_frame_shifts text,
  nextclade_qc_frame_shifts_total_frame_shifts int,
  nextclade_qc_frame_shifts_frame_shifts_ignored text,
  nextclade_qc_frame_shifts_total_frame_shifts_ignored int,
  nextclade_qc_frame_shifts_score float,
  nextclade_qc_frame_shifts_status text,
  nextclade_qc_stop_codons_stop_codons text,
  nextclade_qc_stop_codons_total_stop_codons int,
  nextclade_qc_stop_codons_score float,
  nextclade_qc_stop_codons_status text,
  nextclade_errors text
);

-- Transformed and merged

create table y_main_metadata
(
  id integer primary key,
  source text not null,
  source_primary_key text not null,
  genbank_accession text,
  sra_accession text,
  gisaid_epi_isl text,
  strain text,
  date date,
  year integer,
  month integer,
  day integer,
  date_submitted date,
  region text,
  country text,
  division text,
  location text,
  region_exposure	text,
  country_exposure text,
  division_exposure text,
  host text,
  age int,
  sex text,
  hospitalized boolean,
  died boolean,
  fully_vaccinated boolean,
  sampling_strategy text,
  pango_lineage text,
  nextclade_pango_lineage text,
  nextstrain_clade text,
  gisaid_clade text,
  originating_lab text,
  submitting_lab text,
  authors text,
  nextclade_qc_overall_score float,
  nextclade_qc_missing_data_score float,
  nextclade_qc_mixed_sites_score float,
  nextclade_qc_private_mutations_score float,
  nextclade_qc_snp_clusters_score float,
  nextclade_qc_frame_shifts_score float,
  nextclade_qc_stop_codons_score float
);

create index on y_main_metadata (genbank_accession);
create index on y_main_metadata (sra_accession);
create index on y_main_metadata (gisaid_epi_isl);
create index on y_main_metadata (strain);
create index on y_main_metadata (date);
create index on y_main_metadata (year);
create index on y_main_metadata (month);
create index on y_main_metadata (day);
create index on y_main_metadata (date_submitted);
create index on y_main_metadata (region);
create index on y_main_metadata (country);
create index on y_main_metadata (division);
create index on y_main_metadata (region_exposure);
create index on y_main_metadata (country_exposure);
create index on y_main_metadata (division_exposure);
create index on y_main_metadata (host);
create index on y_main_metadata (age);
create index on y_main_metadata (sex);
create index on y_main_metadata (hospitalized);
create index on y_main_metadata (died);
create index on y_main_metadata (fully_vaccinated);
create index on y_main_metadata (sampling_strategy);
create index on y_main_metadata (pango_lineage);
create index on y_main_metadata (nextclade_pango_lineage);
create index on y_main_metadata (nextstrain_clade);
create index on y_main_metadata (gisaid_clade);
create index on y_main_metadata (originating_lab);
create index on y_main_metadata (submitting_lab);

create table y_main_sequence
(
  id integer primary key,
  seq_original_compressed bytea,
  seq_aligned_compressed bytea,
  aa_mutations text,
  aa_unknowns text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text,
  nuc_unknowns text
);

create table y_main_sequence_columnar
(
  position integer primary key,
  data_compressed bytea not null
);

create table y_main_aa_sequence
(
  id integer not null,
  gene text not null,
  aa_seq_compressed bytea not null,
  primary key (id, gene)
);

create table y_main_aa_sequence_columnar
(
  gene text,
  position integer,
  data_compressed bytea not null,
  primary key (gene, position)
);
