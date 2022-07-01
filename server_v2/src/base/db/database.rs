use crate::base::constants::NucCode;
use crate::base::{util, DataType, SchemaConfig, SchemaConfigMetadata};
use crate::db::get_db_client;
use crate::{DatabaseConfig, SeqCompressor};
use chrono::NaiveDate;
use std::collections::HashMap;

pub struct Database {
    pub number_entries: usize,
    pub metadata: HashMap<String, Column>,
    pub db_config: DatabaseConfig,
    pub column_schema: Vec<SchemaConfigMetadata>,
    pub column_schema_map: HashMap<String, SchemaConfigMetadata>,
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

        let sql = "select * from main_metadata order by id;";
        let mut client = get_db_client(db_config);
        let mut metadata = HashMap::new();
        let mut number_entries = 0;
        for attr in &all_columns {
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
            for attr in &all_columns {
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
                    _ => {}
                }
            }
        }
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
