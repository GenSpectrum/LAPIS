use crate::base::SchemaConfig;
use crate::DatabaseConfig;
use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct ProgramConfig {
    pub database: DatabaseConfig,
    pub schema: SchemaConfig,
}
