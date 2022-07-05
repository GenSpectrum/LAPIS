use crate::base::constants::NucCode;
use crate::base::proc::mutation_finder;
use crate::base::util::{hash_md5, hash_md5_for_hash_map, open_file_with_guessed_compression};
use crate::base::{DataType, DatabaseConfig, SchemaConfig};
use crate::{db, ExecutorService, ProgramConfig, SeqCompressor};
use bio::io::fasta;
use chrono::{Datelike, NaiveDate};
use postgres::types::ToSql;
use regex::Regex;
use std::collections::HashMap;
use std::io::Read;
use std::path::Path;

/// Expects the following three files in `data_dir`:
/// - metadata.tsv
/// - sequences.fasta
/// - aligned.fasta
pub fn load_source_data(data_dir: &Path, config: &ProgramConfig, seq_compressor: &mut SeqCompressor) {
    let path_metadata = data_dir.join(Path::new("metadata.tsv.gz"));
    load_metadata(open_file_with_guessed_compression(&path_metadata).unwrap(), config);

    let path_seq_original = data_dir.join(Path::new("sequences.fasta.xz"));
    load_sequences_original(
        open_file_with_guessed_compression(&path_seq_original).unwrap(),
        config,
        seq_compressor,
    );

    let path_seq_aligned = data_dir.join(Path::new("aligned.fasta.xz"));
    load_sequences_aligned(
        open_file_with_guessed_compression(&path_seq_aligned).unwrap(),
        config,
        seq_compressor,
    );
}

/// Loads metadata.tsv
fn load_metadata<R: Read>(reader: R, config: &ProgramConfig) {
    let ProgramConfig { schema, database, .. } = config;
    let insert_sql = format!(
        "
insert into source_data (metadata_hash, date, year, month, day, date_original, {})
values ($1, $2, $3, $4, $5, $6, {})
on conflict ({}) do update
set
  metadata_hash = $1,
  date = $2,
  year = $3,
  month  = $4,
  day  = $5,
  date_original = $6,
  {};
    ",
        schema
            .additional_metadata
            .iter()
            .map(|x| x.name.as_str())
            .collect::<Vec<_>>()
            .join(", "),
        schema
            .additional_metadata
            .iter()
            .enumerate()
            .map(|(i, _)| format!("${}", 7 + i))
            .collect::<Vec<_>>()
            .join(", "),
        schema.primary_key,
        schema
            .additional_metadata
            .iter()
            .enumerate()
            .map(|(i, x)| format!("{} = ${}", x.name, 7 + i))
            .collect::<Vec<_>>()
            .join(",\n  "),
    );

    // The inserts to the database will be performed in parallel.
    let mut executor_service = ExecutorService::new(20);
    let mut reader = csv::ReaderBuilder::new().delimiter(b'\t').from_reader(reader);

    let mut record_buffer = Vec::with_capacity(1000);
    for result in reader.deserialize() {
        let record: HashMap<String, String> = result.unwrap();
        record_buffer.push(record);
        if record_buffer.len() >= 1000 {
            let records = record_buffer;
            let task = create_load_metadata_task(records, schema.clone(), database.clone(), insert_sql.clone());
            executor_service.submit_task(task);
            record_buffer = Vec::with_capacity(1000);
        }
    }
    if !record_buffer.is_empty() {
        let task = create_load_metadata_task(record_buffer, schema.clone(), database.clone(), insert_sql.clone());
        executor_service.submit_task(task);
    }
    executor_service.close();
    executor_service.join();
}

fn create_load_metadata_task(
    records: Vec<HashMap<String, String>>,
    schema: SchemaConfig,
    db_config: DatabaseConfig,
    insert_sql: String,
) -> Box<dyn 'static + FnOnce(usize) + Send> {
    Box::new(move |_| {
        let mut db_client = db::get_db_client(&db_config);
        let statement = db_client.prepare(insert_sql.as_str()).unwrap();
        for record in records {
            // Skip the entry if the primary key is missing
            let primary_key = handle_null(record.get(&schema.primary_key).unwrap());
            if let None = primary_key {
                continue;
            }

            // The values to be inserted
            let mut values: Vec<&(dyn ToSql + Sync)> = Vec::new();

            // Hash record
            let hash = hash_md5_for_hash_map(&record);
            values.push(&hash);

            // Handle date
            let date_string = handle_null(record.get("date").unwrap());
            let (date, year, month, day) = date_string
                .as_ref()
                .map(|x| parse_date(x.as_str()))
                .unwrap_or((None, None, None, None));
            values.push(&date);
            values.push(&year);
            values.push(&month);
            values.push(&day);
            values.push(&date_string);

            let mut record_vec_parsed: Vec<Option<Box<dyn ToSql + Sync>>> = Vec::new();
            for attr in &schema.additional_metadata {
                let s = record.get(&attr.name).unwrap();
                let nullable = handle_null(s);
                record_vec_parsed.push(match nullable {
                    None => None,
                    Some(x) => Some(match attr.data_type {
                        DataType::String => Box::new(x),
                        DataType::Integer => Box::new(x.parse::<i32>().expect(&format!("Invalid integer: {}", x))),
                        DataType::Date => Box::new(
                            NaiveDate::parse_from_str(x.as_str(), "%Y-%m-%d").expect(&format!("Invalid date: {}", x)),
                        ),
                    }),
                });
            }
            for (i, value) in record_vec_parsed.iter().enumerate() {
                values.push(match value {
                    None => {
                        let data_type = &schema.additional_metadata.get(i).unwrap().data_type;
                        match data_type {
                            DataType::String => &(Option::<String>::None),
                            DataType::Integer => &(Option::<i32>::None),
                            DataType::Date => &(Option::<NaiveDate>::None),
                        }
                    }
                    Some(x) => x.as_ref(),
                });
            }
            db_client.execute(&statement, &values[..]).unwrap();
        }
    })
}

fn load_sequences_original<R: Read>(reader: R, config: &ProgramConfig, seq_compressor: &mut SeqCompressor) {
    let insert_sql = format!(
        "
insert into source_data ({}, seq_original_compressed, seq_original_hash)
values ($1, $2, $3)
on conflict ({}) do update
set
  seq_original_compressed = $2,
  seq_original_hash = $3;
        ",
        config.schema.primary_key, config.schema.primary_key
    );

    let mut db_client = db::get_db_client(&config.database);
    let statement = db_client.prepare(insert_sql.as_str()).unwrap();
    let reader = fasta::Reader::new(reader);
    for result in reader.records() {
        let record = result.unwrap();
        let hash = hash_md5(&record.seq());
        let compressed_seq = seq_compressor.compress_bytes(record.seq());
        db_client
            .execute(&statement, &[&record.id(), &compressed_seq, &hash])
            .unwrap();
    }
}

/// We also determine and import the mutations when loading the aligned sequences
fn load_sequences_aligned<R: Read>(reader: R, config: &ProgramConfig, seq_compressor: &mut SeqCompressor) {
    let insert_sql = format!(
        "
insert into source_data ({}, seq_aligned_compressed, seq_aligned_hash, nuc_substitutions, nuc_deletions, nuc_unknowns)
values ($1, $2, $3, $4, 5, $6)
on conflict ({}) do update
set
  seq_aligned_compressed = $2,
  seq_aligned_hash = $3,
  nuc_substitutions = $4,
  nuc_deletions = $5,
  nuc_unknowns = $6;
        ",
        config.schema.primary_key, config.schema.primary_key
    );

    // The inserts to the database will be performed in parallel.
    let mut executor_service = ExecutorService::new(20);

    let reader = fasta::Reader::new(reader);
    let ref_seq = NucCode::from_seq_string(&config.ref_genome.sequence).unwrap();
    let mut record_buffer = Vec::with_capacity(1000);
    for result in reader.records() {
        let record = result.unwrap();
        record_buffer.push(record);
        if record_buffer.len() >= 1000 {
            let records = record_buffer;
            let task = create_load_sequences_aligned_task(
                records,
                config.database.clone(),
                insert_sql.clone(),
                seq_compressor.clone(),
                ref_seq.clone(),
            );
            executor_service.submit_task(task);
            record_buffer = Vec::with_capacity(1000);
        }
    }
    if !record_buffer.is_empty() {
        let task = create_load_sequences_aligned_task(
            record_buffer,
            config.database.clone(),
            insert_sql.clone(),
            seq_compressor.clone(),
            ref_seq.clone(),
        );
        executor_service.submit_task(task);
    }
    executor_service.close();
    executor_service.join();
}

fn create_load_sequences_aligned_task(
    records: Vec<fasta::Record>,
    db_config: DatabaseConfig,
    insert_sql: String,
    mut seq_compressor: SeqCompressor,
    ref_seq: Vec<NucCode>,
) -> Box<dyn 'static + FnOnce(usize) + Send> {
    Box::new(move |_| {
        let mut db_client = db::get_db_client(&db_config);
        let statement = db_client.prepare(insert_sql.as_str()).unwrap();

        for record in records {
            let hash = hash_md5(&record.seq());
            let compressed_seq = seq_compressor.compress_bytes(record.seq());

            let mut nuc_substitutions = None;
            let mut nuc_deletions = None;
            let mut nuc_unknowns = None;
            let seq = NucCode::from_seq_bytes(record.seq());
            if let Some(seq) = seq {
                let mutations = mutation_finder::find_nuc_mutations(seq.clone(), &ref_seq);
                let mut _nuc_substitutions = Vec::new();
                let mut _nuc_deletions = Vec::new();
                for mutation in &mutations {
                    let ref_code = ref_seq.get(mutation.position - 1).unwrap();
                    let formatted = format!(
                        "{}{}{}",
                        ref_code.to_string(),
                        mutation.position,
                        mutation.to.to_string()
                    );
                    if mutation.to == NucCode::GAP {
                        _nuc_deletions.push(formatted);
                    } else {
                        _nuc_substitutions.push(formatted);
                    }
                }
                nuc_substitutions = Some(_nuc_substitutions.join(","));
                nuc_deletions = Some(_nuc_deletions.join(","));
                let unknowns_positions = mutation_finder::find_nuc_unknowns(seq);
                nuc_unknowns = Some(mutation_finder::compress_positions_as_strings(&unknowns_positions).join(","));
            }

            db_client
                .execute(
                    &statement,
                    &[
                        &record.id(),
                        &compressed_seq,
                        &hash,
                        &nuc_substitutions,
                        &nuc_deletions,
                        &nuc_unknowns,
                    ],
                )
                .unwrap();
        }
    })
}

fn handle_null(s: &str) -> Option<String> {
    if s == "" || s == "?" || s == "NA" {
        None
    } else {
        Some(s.to_string())
    }
}

fn parse_date(s: &str) -> (Option<NaiveDate>, Option<i32>, Option<i32>, Option<i32>) {
    let parsed_date_opt = NaiveDate::parse_from_str(s, "%Y-%m-%d").ok();
    if let Some(parsed_date) = parsed_date_opt {
        // If the date is complete
        (
            parsed_date_opt,
            Some(parsed_date.year()),
            Some(parsed_date.month() as i32),
            Some(parsed_date.day() as i32),
        )
    } else {
        // Parse partial dates
        // TODO(perf) This branch is quite expensive and should be improved for big datasets with many partial dates
        let tmp = s.replace("X", "0");
        let re = Regex::new(r"(\d{4})(-\d{2})?(-\d{2})?").unwrap();
        let regex_match = re.captures(tmp.as_str());
        match regex_match {
            None => (None, None, None, None),
            Some(m) => {
                let year = m
                    .get(1)
                    .map(|x| match x.as_str().parse::<i32>().ok() {
                        None => None,
                        Some(x) => {
                            if x > 0 {
                                Some(x)
                            } else {
                                None
                            }
                        }
                    })
                    .flatten();
                let month = m
                    .get(2)
                    .map(|x| match x.as_str()[1..].parse::<i32>().ok() {
                        None => None,
                        Some(x) => {
                            if x > 0 && x <= 12 {
                                Some(x)
                            } else {
                                None
                            }
                        }
                    })
                    .flatten();
                let day = m
                    .get(3)
                    .map(|x| match x.as_str()[1..].parse::<i32>().ok() {
                        None => None,
                        Some(x) => {
                            if x > 0 && x <= 31 {
                                Some(x)
                            } else {
                                None
                            }
                        }
                    })
                    .flatten();
                (None, year, month, day)
            }
        }
    }
}
