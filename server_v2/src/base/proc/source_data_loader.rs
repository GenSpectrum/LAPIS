use crate::base::{seq_compression, SchemaConfig};
use crate::{db, DatabaseConfig, SeqCompressor};
use bio::io::fasta;
use chrono::NaiveDate;
use postgres::types::ToSql;
use regex::Regex;
use std::collections::HashMap;
use std::fs::File;
use std::path::Path;

/// Expects the following three files in `data_dir`:
/// - metadata.tsv
/// - sequences.fasta
/// - aligned.fasta
pub fn load_source_data(
    data_dir: &Path,
    schema: &SchemaConfig,
    db_config: &DatabaseConfig,
    seq_compressor: &mut SeqCompressor,
) {
    // load_metadata(&data_dir.join(Path::new("metadata.tsv")), schema, db_config);
    load_sequences(
        "original",
        &data_dir.join(Path::new("sequences.fasta")),
        schema,
        db_config,
        seq_compressor,
    );
    load_sequences(
        "aligned",
        &data_dir.join(Path::new("aligned.fasta")),
        schema,
        db_config,
        seq_compressor,
    );
}

/// Loads metadata.tsv
fn load_metadata(file_path: &Path, schema: &SchemaConfig, db_config: &DatabaseConfig) {
    let insert_sql = format!(
        "
insert into source_data (metadata_hash, date, year, month, day, date_original, {})
values (null, $1, $2, $3, $4, $5, {})
on conflict ({}) do update
set
  metadata_hash = null,
  date = $1,
  year = $2,
  month  = $3,
  day  = $4,
  date_original = $5,
  {};
    ",
        schema
            .additional_metadata
            .iter()
            .enumerate()
            .map(|(i, x)| x.name.as_str())
            .collect::<Vec<_>>()
            .join(", "),
        schema
            .additional_metadata
            .iter()
            .enumerate()
            .map(|(i, x)| format!("${}", 6 + i))
            .collect::<Vec<_>>()
            .join(", "),
        schema.primary_key,
        schema
            .additional_metadata
            .iter()
            .enumerate()
            .map(|(i, x)| format!("{} = ${}", x.name, 6 + i))
            .collect::<Vec<_>>()
            .join(",\n  "),
    );

    let mut db_client = db::get_db_client(db_config);
    type Record = HashMap<String, String>;
    let file = File::open(file_path).unwrap();
    let mut reader = csv::ReaderBuilder::new().delimiter(b'\t').from_reader(file);
    let statement = db_client.prepare(insert_sql.as_str()).unwrap();
    for result in reader.deserialize() {
        let record: Record = result.unwrap();
        let mut x: Vec<&(dyn ToSql + Sync)> = Vec::new();
        let date_string = handle_null(record.get("date").unwrap());
        let (date, year, month, day) = date_string
            .as_ref()
            .map(|x| parse_date(x.as_str()))
            .unwrap_or((None, None, None, None));
        x.push(&date);
        x.push(&year);
        x.push(&month);
        x.push(&day);
        x.push(&date_string);

        let mut record_vec_parsed: Vec<Option<Box<dyn ToSql + Sync>>> = Vec::new();
        for attr in &schema.additional_metadata {
            let s = record.get(&attr.name).unwrap();
            let nullable = handle_null(s);
            record_vec_parsed.push(match nullable {
                None => None,
                Some(x) => Some(Box::new(x)),
            });
        }
        for value in &record_vec_parsed {
            x.push(match value {
                None => &(Option::<String>::None),
                Some(x) => x.as_ref(),
            });
        }
        db_client.execute(&statement, &x[..]).unwrap();
    }
    println!("Done");
}

/// sequence_type: "original" or "aligned"
fn load_sequences(
    sequence_type: &str,
    file_path: &Path,
    schema: &SchemaConfig,
    db_config: &DatabaseConfig,
    seq_compressor: &mut SeqCompressor,
) {
    let insert_sql = format!(
        "
insert into source_data ({}, seq_{}_compressed, seq_{}_hash)
values ($1, $2, null)
on conflict ({}) do update
set
  seq_{}_compressed = $2,
  seq_{}_hash = null;
        ",
        schema.primary_key,
        sequence_type,
        sequence_type,
        schema.primary_key,
        sequence_type,
        sequence_type
    );

    let mut db_client = db::get_db_client(db_config);
    let statement = db_client.prepare(insert_sql.as_str()).unwrap();
    let file = File::open(file_path).unwrap();
    let mut reader = fasta::Reader::new(file);
    for result in reader.records() {
        let record = result.unwrap();
        let compressed_seq = seq_compressor.compress_bytes(record.seq());
        db_client
            .execute(&statement, &[&record.id(), &compressed_seq])
            .unwrap();
    }
}

fn handle_null(s: &str) -> Option<String> {
    if s == "" {
        None
    } else {
        Some(s.to_string())
    }
}

fn parse_date(s: &str) -> (Option<NaiveDate>, Option<i32>, Option<i32>, Option<i32>) {
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
            let parsed_date = NaiveDate::parse_from_str(s, "%Y-%m-%d").ok();
            (parsed_date, year, month, day)
        }
    }
}
