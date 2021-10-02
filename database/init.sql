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
  aa_seqs text,
  aa_mutations text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text
);


-- Source: GISAID

create table y_gisaid
(
  updated_at timestamp not null,
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
  aa_seqs text,
  aa_mutations text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text
);


-- Source: S3C (additional metadata)

create table y_s3c (
  gisaid_epi_isl text unique,
  sra_accession text unique,
  hospitalized boolean,
  died boolean,
  fully_vaccinated boolean
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
  aa_seq text not null,
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
  y_main_metadata,
  y_main_sequence,
  y_main_sequence_columnar,
  y_main_aa_sequence,
  y_main_aa_sequence_columnar
to y_user;
