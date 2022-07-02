use crate::base::constants::NucCode;
use crate::base::{util, DataType, SchemaConfig, SchemaConfigMetadata};
use crate::db::Mutation;
use crate::{DatabaseConfig, MutationStore, SeqCompressor};
use chrono::{Local, NaiveDate};
use postgres::{Client, NoTls};
use postgres_cursor::Cursor;
use std::collections::HashMap;

pub fn get_db_client(config: &DatabaseConfig) -> Client {
    let mut db_config = postgres::config::Config::new();
    db_config
        .host(&config.host)
        .port(config.port)
        .user(&config.username)
        .password(&config.password)
        .dbname(&config.dbname);
    let mut client = db_config.connect(NoTls).expect("Database connection failed");
    client
        .execute(&format!("set search_path to '{}'", &config.schema), &[])
        .expect("The search_path of the database could not be changed.");
    client
}

pub struct Database {
    pub number_entries: usize,
    pub metadata: HashMap<String, Column>,
    pub db_config: DatabaseConfig,
    pub column_schema: Vec<SchemaConfigMetadata>,
    pub column_schema_map: HashMap<String, SchemaConfigMetadata>,
    pub nuc_mutation_store: MutationStore,
}

#[derive(Debug)]
pub enum Column {
    Str(Vec<Option<String>>),
    Int(Vec<Option<i32>>),
}

impl Database {
    pub fn load(schema: &SchemaConfig, db_config: &DatabaseConfig) -> Database {
        let mut all_columns = schema.additional_metadata.clone();
        all_columns.push(SchemaConfigMetadata {
            name: "date".to_string(),
            data_type: DataType::Date,
        });
        all_columns.push(SchemaConfigMetadata {
            name: "year".to_string(),
            data_type: DataType::Integer,
        });
        all_columns.push(SchemaConfigMetadata {
            name: "month".to_string(),
            data_type: DataType::Integer,
        });
        all_columns.push(SchemaConfigMetadata {
            name: "day".to_string(),
            data_type: DataType::Integer,
        });

        let (number_entries, metadata) = load_metadata(db_config, &all_columns);
        let (nuc_mutation_store, _) = load_mutations(db_config, number_entries as u32);

        let mut column_schema_map = HashMap::new();
        for column in &all_columns {
            column_schema_map.insert(column.name.clone(), column.clone());
        }
        Database {
            number_entries,
            metadata,
            db_config: db_config.clone(),
            column_schema: all_columns,
            column_schema_map,
            nuc_mutation_store,
        }
    }

    pub fn load_nuc_column(&self, position: u32) -> Vec<NucCode> {
        let sql = "select data_compressed from main_sequence_columnar where position = $1;";
        let mut client = get_db_client(&self.db_config);
        let rows = client.query(sql, &[&(position as i32)]).unwrap();
        let data = rows.get(0);
        match data {
            None => panic!("No data is available for nucleotide position {}", position),
            Some(row) => {
                let compressed: Vec<u8> = row.get("data_compressed");
                SeqCompressor::new()
                    .decompress(&compressed)
                    .iter()
                    .map(|x| NucCode::from_byte_ignore_weird(x))
                    .collect()
            }
        }
    }
}

fn load_metadata(
    db_config: &DatabaseConfig,
    all_columns: &Vec<SchemaConfigMetadata>,
) -> (usize, HashMap<String, Column>) {
    let sql = "select * from main_metadata order by id;";
    let mut client = get_db_client(db_config);
    let mut metadata = HashMap::new();
    let mut number_entries: usize = 0;
    for attr in all_columns {
        match attr.data_type {
            DataType::String => {
                metadata.insert(attr.name.to_string(), Column::Str(Vec::new()));
            }
            DataType::Integer | DataType::Date => {
                metadata.insert(attr.name.to_string(), Column::Int(Vec::new()));
            }
        }
    }
    for row in client.query(sql, &[]).unwrap() {
        number_entries += 1;
        for attr in all_columns {
            match attr.data_type {
                DataType::String => {
                    let column = metadata.get_mut(attr.name.as_str()).unwrap();
                    match column {
                        Column::Str(v) => {
                            v.push(row.get(attr.name.as_str()));
                        }
                        _ => panic!("Unexpected column type"),
                    }
                }
                DataType::Integer => {
                    let column = metadata.get_mut(attr.name.as_str()).unwrap();
                    match column {
                        Column::Int(v) => {
                            v.push(row.get(attr.name.as_str()));
                        }
                        _ => panic!("Unexpected column type"),
                    }
                }
                DataType::Date => {
                    let column = metadata.get_mut(attr.name.as_str()).unwrap();
                    match column {
                        Column::Int(v) => {
                            let date: Option<NaiveDate> = row.get(attr.name.as_str());
                            v.push(match date {
                                None => None,
                                Some(date) => Some(util::encode_date_as_int(&date)),
                            });
                        }
                        _ => panic!("Unexpected column type"),
                    }
                }
            }
        }
    }
    (number_entries, metadata)
}

fn load_mutations(db_config: &DatabaseConfig, size: u32) -> (MutationStore, HashMap<String, MutationStore>) {
    let mut client = get_db_client(db_config);
    let GENES: Vec<String> = vec![];
    let mut nuc_mutation_store = MutationStore::with_capacity(size);
    let mut aa_mutation_stores = HashMap::new();
    for gene in &GENES {
        aa_mutation_stores.insert(gene.to_string(), MutationStore::with_capacity(size));
    }

    let sequence_sql = "
        select
          id,
          coalesce(aa_mutations, '') as aa_mutations,
          coalesce(aa_unknowns, '') as aa_unknowns,
          coalesce(nuc_substitutions, '') as nuc_substitutions,
          coalesce(nuc_deletions, '') as nuc_deletions,
          coalesce(nuc_unknowns, '') as nuc_unknowns
        from main_sequence
        order by id
    ";
    let mut cursor = Cursor::build(&mut client)
        .batch_size(500000)
        .query(sequence_sql)
        .finalize()
        .expect("cursor creation succeeded");

    let mut i = 0;
    for result in &mut cursor {
        let rows = result.unwrap();
        for row in &rows {
            if i % 10000 == 0 {
                println!("{} {}/{}", Local::now(), i / 10000, size / 10000);
            }
            i += 1;
            // Nuc mutations
            let nuc_substitutions: String = row.get("nuc_substitutions");
            let nuc_deletions: String = row.get("nuc_deletions");
            let nuc_unknowns_string: String = row.get("nuc_unknowns");
            let nuc_mutations: Vec<Mutation> = nuc_substitutions
                .split(",")
                .chain(nuc_deletions.split(","))
                .filter(|x| !x.is_empty())
                .map(|x| x.parse().unwrap())
                .collect();
            let nuc_unknowns: Vec<&str> = nuc_unknowns_string.split(",").filter(|x| !x.is_empty()).collect();
            nuc_mutation_store.push(&nuc_mutations, &nuc_unknowns);
            // AA mutations
            let aa_mutations: String = row.get("aa_mutations");
            let aa_unknowns: String = row.get("aa_unknowns");
            let mut aa_mutations_per_gene: HashMap<String, Vec<Mutation>> = HashMap::new();
            let mut aa_unknowns_per_gene: HashMap<String, Vec<&str>> = HashMap::new();
            for gene in &GENES {
                aa_mutations_per_gene.insert(gene.to_string(), Vec::new());
                aa_unknowns_per_gene.insert(gene.to_string(), Vec::new());
            }
            aa_mutations.split(",").filter(|x| !x.is_empty()).for_each(|x| {
                let parts: Vec<&str> = x.split(":").collect();
                let mutation: Mutation = parts[1].parse().unwrap();
                aa_mutations_per_gene.get_mut(parts[0]).unwrap().push(mutation);
            });
            aa_unknowns.split(",").filter(|x| !x.is_empty()).for_each(|x| {
                let parts: Vec<&str> = x.split(":").collect();
                aa_unknowns_per_gene.get_mut(parts[0]).unwrap().push(parts[1]);
            });
            for (gene, aa_mutation_store) in &mut aa_mutation_stores {
                aa_mutation_store.push(
                    aa_mutations_per_gene.get(gene).unwrap(),
                    aa_unknowns_per_gene.get(gene).unwrap(),
                );
            }
        }
    }

    (nuc_mutation_store, aa_mutation_stores)
}
