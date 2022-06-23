use crate::DatabaseConfig;
use serde::Deserialize;
use crate::base::SchemaConfig;

#[derive(Debug, Deserialize)]
pub struct ProgramConfig {
    pub database: DatabaseConfig,
    pub schema: SchemaConfig
}
