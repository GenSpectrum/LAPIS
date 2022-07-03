use serde::Deserialize;

#[derive(Debug, Deserialize, Clone)]
pub struct ProgramConfig {
    pub database: DatabaseConfig,
    pub schema: SchemaConfig,
    pub ref_genome: RefGenomeConfig,
}

#[derive(Debug, Deserialize, Clone)]
pub struct DatabaseConfig {
    pub host: String,
    pub port: u16,
    pub dbname: String,
    pub username: String,
    pub password: String,
    pub schema: String,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct SchemaConfig {
    pub instance_name: String,
    pub additional_metadata: Vec<SchemaConfigMetadata>,
    pub primary_key: String,
}

#[derive(Debug, Deserialize, Clone)]
pub struct SchemaConfigMetadata {
    pub name: String,
    #[serde(rename = "type")]
    pub data_type: DataType,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub enum DataType {
    String,
    Integer,
    Date,
}

#[derive(Debug, Deserialize, Clone)]
pub struct RefGenomeConfig {
    pub name: String,
    pub sequence: String,
}
