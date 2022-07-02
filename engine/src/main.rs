extern crate core;

mod base;

use crate::base::db::{DatabaseConfig, MutationStore};
use crate::base::proc::SequenceRowToColumnTransformer;
use crate::base::seq_compression::SeqCompressor;
use crate::base::util::ExecutorService;
use crate::base::{db, mutation, RefGenomeConfig};
use crate::base::{server, ProgramConfig};
use crate::db::{filters, Database};
use crate::filters::Filter;
use chrono::Local;
use config::{Config, File, FileFormat};
use std::path::Path;
use std::sync::Arc;

fn main() {
    println!("{} Welcome", Local::now());

    let config = read_config();
    let ref_genome_config = read_ref_genome_config();
    let mut nuc_seq_compressor = SeqCompressor::with_dict(ref_genome_config.sequence.as_bytes());

    db::generate_db_tables(&config.database, &config.schema);
    // base::proc::load_source_data(
    //     Path::new("/Users/chachen/polybox/tmp_mpox/data"),
    //     &config.schema,
    //     &config.database,
    // );
    base::proc::load_source_data(
        Path::new("E:/polybox/tmp_mpox/data"),
        &config.schema,
        &config.database,
        &ref_genome_config,
        &mut nuc_seq_compressor,
    );
    base::proc::source_to_main(&config.schema, &config.database, &mut nuc_seq_compressor);

    let db = Arc::new(Database::load(&config.schema, &config.database));
    server::main(db).unwrap();

    println!("Done");
}

fn read_config() -> ProgramConfig {
    let builder = Config::builder().add_source(File::new("config.yml", FileFormat::Yaml));
    let config_unparsed = builder.build().expect("The config file cannot be loaded.");
    let config: ProgramConfig = config_unparsed.try_deserialize().expect("The config file is invalid.");
    config
}

fn read_ref_genome_config() -> RefGenomeConfig {
    let builder = Config::builder().add_source(File::new("config_ref_genome.yml", FileFormat::Yaml));
    let config_unparsed = builder.build().expect("The ref genome config file cannot be loaded.");
    let config: RefGenomeConfig = config_unparsed
        .try_deserialize()
        .expect("The ref genome config file is invalid.");
    config
}
