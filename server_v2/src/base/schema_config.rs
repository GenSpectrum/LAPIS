use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct SchemaConfig {
    #[serde(rename = "instanceName")]
    pub instance_name: String,
    #[serde(rename = "additionalMetadata")]
    pub additional_metadata: Vec<SchemaConfigMetadata>,
    #[serde(rename = "primaryKey")]
    pub primary_key: String,
}

#[derive(Debug, Deserialize)]
pub struct SchemaConfigMetadata {
    pub name: String,
    #[serde(rename = "type")]
    pub data_type: String,
}
