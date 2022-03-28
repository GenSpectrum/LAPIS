create schema open;
create schema gisaid;

grant usage on schema open, gisaid to y_user;

-- Basic information

create table pangolin_lineage_alias
(
  alias text primary key,
  full_name text not null unique
);


create table data_version
(
  dataset text primary key,
  timestamp bigint not null
);


-- Source: Nextstrain/GenBank

create table y_nextstrain_genbank
(
  metadata_hash text,
  seq_original_hash text,
  seq_aligned_hash text,
  genbank_accession text,
  sra_accession text,
  gisaid_epi_isl text,
  strain text primary key,
  date date,
  date_original text,
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
  sampling_strategy text,
  pango_lineage text,
  nextstrain_clade text,
  gisaid_clade text,
  originating_lab text,
  submitting_lab text,
  authors text,
  seq_original_compressed bytea,
  seq_aligned_compressed bytea,
  aa_seqs_compressed bytea,
  aa_mutations text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text
);


-- Source: GISAID

create table y_gisaid
(
  updated_at timestamp not null,
  metadata_hash text,
  seq_original_hash text,
  gisaid_epi_isl text primary key,
  strain text,
  date date,
  date_original text,
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
  sampling_strategy text,
  pango_lineage text,
  gisaid_clade text,
  originating_lab text,
  submitting_lab text,
  authors text,
  seq_original_compressed bytea,
  seq_aligned_compressed bytea,
  aa_seqs_compressed bytea,
  aa_mutations text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text,

  -- Nextclade stuff
  nextclade_clade text,
  nextclade_pango_lineage text,
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
  nextclade_pcr_primer_changes int,
  nextclade_alignment_score int,
  nextclade_alignment_start int,
  nextclade_alignment_end int,
  nextclade_qc_overall_score float,
  nextclade_qc_overall_status text,
  nextclade_qc_missing_data_missing_data_threshold int,
  nextclade_qc_missing_data_score float,
  nextclade_qc_missing_data_status text,
  nextclade_qc_missing_data_total_missing int,
  nextclade_qc_mixed_sites_mixed_sites_threshold int,
  nextclade_qc_mixed_sites_score int,
  nextclade_qc_mixed_sites_status text,
  nextclade_qc_mixed_sites_total_mixed_sites int,
  nextclade_qc_private_mutations_cutoff int,
  nextclade_qc_private_mutations_excess int,
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
  nextclade_qc_frame_shifts_score int,
  nextclade_qc_frame_shifts_status text,
  nextclade_qc_stop_codons_stop_codons text,
  nextclade_qc_stop_codons_total_stop_codons int,
  nextclade_qc_stop_codons_score int,
  nextclade_qc_stop_codons_status text,
  nextclade_errors text
);


-- Source: S3C (additional metadata)

create table y_s3c (
  gisaid_epi_isl text unique,
  sra_accession text unique,
  age integer,
  sex text,
  hospitalized boolean,
  died boolean,
  fully_vaccinated boolean
);


-- Source: cov-lineages GitHub pangolin-assignments
-- https://github.com/cov-lineages/pangolin-assignment

create table y_pangolin_assignment (
  gisaid_epi_isl text primary key,
  pango_lineage text
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
  nextstrain_clade text,
  gisaid_clade text,
  originating_lab text,
  submitting_lab text,
  authors text
);

create index on y_main_metadata (genbank_accession);
create index on y_main_metadata (sra_accession);
create index on y_main_metadata (gisaid_epi_isl);
create index on y_main_metadata (strain);
create index on y_main_metadata (date);
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
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text
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


-- Privileges

grant select, insert, update, delete, references, truncate
on
  pangolin_lineage_alias,
  data_version,
  y_nextstrain_genbank,
--   y_gisaid,
  y_s3c,
  y_pangolin_assignment,
  y_main_metadata,
  y_main_sequence,
  y_main_sequence_columnar,
  y_main_aa_sequence,
  y_main_aa_sequence_columnar
to y_user;
