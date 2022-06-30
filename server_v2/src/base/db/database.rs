use crate::base::constants::NucCode;
use crate::base::SchemaConfig;
use crate::db::get_db_client;
use crate::{DatabaseConfig, SeqCompressor};
use std::collections::HashMap;

pub struct Database {
    pub number_entries: usize,
    pub metadata: HashMap<String, Column>,
    pub db_config: DatabaseConfig,
}

#[derive(Debug)]
pub enum Column {
    Str(Vec<Option<String>>),
}

impl Database {
    pub fn load(schema: &SchemaConfig, db_config: &DatabaseConfig) -> Database {
        let sql = "select * from main_metadata order by id;";
        let mut client = get_db_client(db_config);
        let mut metadata = HashMap::new();
        let mut number_entries = 0;
        for attr in &schema.additional_metadata {
            match attr.data_type.as_str() {
                "string" => {
                    metadata.insert(attr.name.to_string(), Column::Str(Vec::new()));
                }
                data_type => panic!("Unknown data type: {}", data_type),
            }
        }
        for row in client.query(sql, &[]).unwrap() {
            number_entries += 1;
            for attr in &schema.additional_metadata {
                match attr.data_type.as_str() {
                    "string" => {
                        let column = metadata.get_mut(attr.name.as_str()).unwrap();
                        match column {
                            Column::Str(v) => {
                                v.push(row.get(attr.name.as_str()));
                            }
                        }
                    }
                    _ => {}
                }
            }
        }
        Database {
            number_entries,
            metadata,
            db_config: db_config.clone(),
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
                    .map(|x| NucCode::from_byte_ignore_weird(*x))
                    .collect()
            }
        }
    }
}
