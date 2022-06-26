use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct RefGenomeConfig {
    pub name: String,
    pub sequence: String,
}
