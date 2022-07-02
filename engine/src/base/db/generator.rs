use crate::base::{db, DataType, SchemaConfig};
use crate::DatabaseConfig;

pub fn generate_db_tables(db_config: &DatabaseConfig, schema: &SchemaConfig) {
    let mut client = db::get_db_client(db_config);

    // Generate SQL
    let mut additional_attrs_sql_parts: Vec<String> = Vec::new();
    let mut additional_attrs_sql_parts_with_primary_key: Vec<String> = Vec::new();
    for attr in &schema.additional_metadata {
        let data_type = match attr.data_type {
            DataType::String => "text",
            DataType::Integer => "integer",
            DataType::Date => "date",
        };
        let part = format!("{} {}", attr.name, data_type);
        additional_attrs_sql_parts_with_primary_key.push(format!(
            "{}{}",
            part,
            if attr.name == schema.primary_key {
                " primary key"
            } else {
                ""
            }
        ));
        additional_attrs_sql_parts.push(part);
    }
    let additional_attrs_sql = additional_attrs_sql_parts.join(",\n  ");
    let additional_attrs_with_primary_key_sql = additional_attrs_sql_parts_with_primary_key.join(",\n  ");

    let create_source_table_sql = format!(
        "
create table source_data
(
  metadata_hash text,
  seq_original_hash text,
  seq_aligned_hash text,
  date date,
  year integer,
  month integer,
  day integer,
  date_original text,
  seq_original_compressed bytea,
  seq_aligned_compressed bytea,
  aa_seqs_compressed bytea,
  aa_mutations text,
  aa_unknowns text,
  nuc_substitutions text,
  nuc_deletions text,
  nuc_insertions text,
  nuc_unknowns text,
  {}
);",
        additional_attrs_with_primary_key_sql
    );

    let create_metadata_table = format!(
        "
create table main_metadata
(
  id integer primary key,
  date date,
  year integer,
  month integer,
  day integer,
  {}
);
",
        additional_attrs_sql
    );

    let create_basics_sqls = vec![
        "
create table data_version
(
  dataset text primary key,
  timestamp bigint not null
);",
        "
create table main_sequence
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
);",
        "
create table main_sequence_columnar
(
  position integer primary key,
  data_compressed bytea not null
);",
        "
create table main_aa_sequence
(
  id integer not null,
  gene text not null,
  aa_seq_compressed bytea not null,
  primary key (id, gene)
);",
        "
create table main_aa_sequence_columnar
(
  gene text,
  position integer,
  data_compressed bytea not null,
  primary key (gene, position)
);",
        "create table main_metadata_staging (like main_metadata including all);",
        "create table main_sequence_staging (like main_sequence including all);",
        "create table main_sequence_columnar_staging (like main_sequence_columnar including all);",
        "create table main_aa_sequence_staging (like main_aa_sequence including all);",
        "create table main_aa_sequence_columnar_staging (like main_aa_sequence_columnar including all);",
        "create or replace function switch_in_staging_tables() returns void security definer as $$
begin
  alter table main_metadata rename to main_metadata_old;
  alter table main_sequence rename to main_sequence_old;
  alter table main_sequence_columnar rename to main_sequence_columnar_old;
  alter table main_aa_sequence rename to main_aa_sequence_old;
  alter table main_aa_sequence_columnar rename to main_aa_sequence_columnar_old;

  alter table main_metadata_staging rename to main_metadata;
  alter table main_sequence_staging rename to main_sequence;
  alter table main_sequence_columnar_staging rename to main_sequence_columnar;
  alter table main_aa_sequence_staging rename to main_aa_sequence;
  alter table main_aa_sequence_columnar_staging rename to main_aa_sequence_columnar;

  truncate main_metadata_old, main_sequence_old,
    main_sequence_columnar_old, main_aa_sequence_old,
    main_aa_sequence_columnar_old;

  alter table main_metadata_old rename to main_metadata_staging;
  alter table main_sequence_old rename to main_sequence_staging;
  alter table main_sequence_columnar_old rename to main_sequence_columnar_staging;
  alter table main_aa_sequence_old rename to main_aa_sequence_staging;
  alter table main_aa_sequence_columnar_old rename to main_aa_sequence_columnar_staging;
end;
$$ language plpgsql;",
        "create or replace function switch_in_staging_tables_without_truncate() returns void security definer as $$
begin
  alter table main_metadata rename to main_metadata_old;
  alter table main_sequence rename to main_sequence_old;
  alter table main_sequence_columnar rename to main_sequence_columnar_old;
  alter table main_aa_sequence rename to main_aa_sequence_old;
  alter table main_aa_sequence_columnar rename to main_aa_sequence_columnar_old;

  alter table main_metadata_staging rename to main_metadata;
  alter table main_sequence_staging rename to main_sequence;
  alter table main_sequence_columnar_staging rename to main_sequence_columnar;
  alter table main_aa_sequence_staging rename to main_aa_sequence;
  alter table main_aa_sequence_columnar_staging rename to main_aa_sequence_columnar;

  alter table main_metadata_old rename to main_metadata_staging;
  alter table main_sequence_old rename to main_sequence_staging;
  alter table main_sequence_columnar_old rename to main_sequence_columnar_staging;
  alter table main_aa_sequence_old rename to main_aa_sequence_staging;
  alter table main_aa_sequence_columnar_old rename to main_aa_sequence_columnar_staging;
end;
$$ language plpgsql;",
    ];

    // Execute SQLs
    client
        .execute(create_source_table_sql.as_str(), &[])
        .expect("Table init failed");
    client
        .execute(create_metadata_table.as_str(), &[])
        .expect("Table init failed");
    for create_basics_sql in create_basics_sqls {
        client.execute(create_basics_sql, &[]).expect("Table init failed");
    }
}
