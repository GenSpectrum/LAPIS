use crate::base::{DatabaseConfig, SchemaConfig};
use crate::{db, SeqCompressor, SequenceRowToColumnTransformer};

/// Moves data from the `source_data` table to the `main_` tables and performs the necessary
/// processing including transforming the sequences to a columnar format
pub fn source_to_main(schema: &SchemaConfig, db_config: &DatabaseConfig, seq_compressor: &mut SeqCompressor) {
    copy_data(schema, db_config);
    transform_seqs_to_columnar(db_config, seq_compressor);
    deploy_staging(db_config);
}

fn copy_data(schema: &SchemaConfig, db_config: &DatabaseConfig) {
    let additional_field_names = schema
        .additional_metadata
        .iter()
        .map(|x| x.name.as_str())
        .collect::<Vec<_>>()
        .join(", ");
    let sql1 = format!(
        "
insert into main_metadata_staging (
  id, date, year, month, day, {}
)
select
  row_number() over () - 1 as id,
  date,
  year,
  month,
  day,
  {}
from source_data
where
  seq_aligned_compressed is not null;
        ",
        additional_field_names, additional_field_names,
    );
    let sql2 = format!(
        "
insert into main_sequence_staging (
  id, seq_original_compressed, seq_aligned_compressed, aa_mutations, aa_unknowns, nuc_substitutions,
  nuc_deletions, nuc_insertions, nuc_unknowns
)
select
  mm.id,
  s.seq_original_compressed,
  s.seq_aligned_compressed,
  s.aa_mutations,
  s.aa_unknowns,
  s.nuc_substitutions,
  s.nuc_deletions,
  s.nuc_insertions,
  s.nuc_unknowns
from
  source_data s
  join main_metadata_staging mm on mm.{} = s.{};
        ",
        schema.primary_key, schema.primary_key,
    );

    let mut db_client = db::get_db_client(db_config);
    db_client.execute(sql1.as_str(), &[]).unwrap();
    db_client.execute(sql2.as_str(), &[]).unwrap();
}

fn transform_seqs_to_columnar(db_config: &DatabaseConfig, seq_compressor: &mut SeqCompressor) {
    // Load all compressed and aligned sequences and their IDs
    let sql1 = "
select s.id, s.seq_aligned_compressed
from main_sequence_staging s
order by s.id;
    ";
    let mut compressed_sequences = vec![];
    let mut id_counter = 0;
    let mut db_client = db::get_db_client(db_config);
    for row in db_client.query(sql1, &[]).unwrap() {
        let id: i32 = row.get("id");
        let compressed: Option<Vec<u8>> = row.get("seq_aligned_compressed");
        if id != id_counter {
            panic!("Weird.. I expected ID={} but got {}", id_counter, id);
        }
        compressed_sequences.push(compressed);
        id_counter += 1;
    }
    println!("Number: {}", compressed_sequences.len());
    println!("Data loaded");

    // Transform
    let transformer = SequenceRowToColumnTransformer::new(16, 50000);
    let db_config = db_config.clone();
    let consume = move |pos_offset, transformed_seqs: Vec<Vec<u8>>| {
        let sql2 = "
insert into main_sequence_columnar_staging (position, data_compressed)
values ($1, $2);
        ";
        let mut db_client = db::get_db_client(&db_config);
        let mut transaction = db_client.transaction().unwrap();
        let statement = transaction.prepare(sql2).unwrap();
        for (i, compressed) in transformed_seqs.iter().enumerate() {
            if i % 100 == 0 {
                println!("Pos {} ({}/{})", pos_offset, i, transformed_seqs.len());
            }
            let pos: i32 = (pos_offset + i) as i32;
            transaction.execute(&statement, &[&pos, compressed]).unwrap();
        }
        transaction.commit().unwrap();
    };
    let columnar_compressor = SeqCompressor::new();
    transformer.transform(
        &compressed_sequences,
        &seq_compressor,
        &columnar_compressor,
        consume,
        b'!',
    );
}

fn deploy_staging(db_config: &DatabaseConfig) {
    let sql = "select switch_in_staging_tables();";
    let mut db_client = db::get_db_client(db_config);
    db_client.execute(sql, &[]).unwrap();
}
