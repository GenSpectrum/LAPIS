extern crate core;

mod base;

use crate::base::db::{DatabaseConfig, MutationStore};
use crate::base::proc::SequenceRowToColumnTransformer;
use crate::base::seq_compression::SeqCompressor;
use crate::base::util::ExecutorService;
use crate::base::ProgramConfig;
use crate::base::{db, RefGenomeConfig};
use chrono::Local;
use config::{Config, File, FileFormat};
use std::sync::Mutex;
use std::time;

fn main() {
    println!("{} Welcome", Local::now());
    // todo!("Stop here");

    let config = read_config();
    let ref_genome_config = read_ref_genome_config();
    let mut nuc_seq_compressor = SeqCompressor::with_dict(ref_genome_config.sequence.as_bytes());

    // db::generate_db_tables(&config.database, &config.schema);
    // base::proc::load_source_data(
    //     Path::new("/Users/chachen/polybox/tmp_mpox/data"),
    //     &config.schema,
    //     &config.database,
    // );
    // base::proc::load_source_data(
    //     Path::new("E:/polybox/tmp_mpox/data"),
    //     &config.schema,
    //     &config.database,
    //     &mut nuc_seq_compressor,
    // );
    base::proc::source_to_main(&config.schema, &config.database, &mut nuc_seq_compressor);

    // let db = Database::load(&config.database);
    //
    // for _ in 0..20 {
    //     let ids = random_ids(db.size, 0.2);
    //     let start = time::Instant::now();
    //     let nucs = db.nuc_muts(&ids);
    //     let duration = start.elapsed();
    //     println!("In {} sequences, I found a total of {} nucleotide mutations ({}ms)", ids.len(), nucs.len(), duration.as_millis());
    //     thread::sleep(time::Duration::from_secs(3));
    // }
}

fn read_config() -> ProgramConfig {
    let builder = Config::builder().add_source(File::new("config.yml", FileFormat::Yaml));
    let config_unparsed = builder.build().expect("The config file cannot be loaded.");
    let config: ProgramConfig = config_unparsed
        .try_deserialize()
        .expect("The config file is invalid.");
    config
}

fn read_ref_genome_config() -> RefGenomeConfig {
    let builder =
        Config::builder().add_source(File::new("config_ref_genome.yml", FileFormat::Yaml));
    let config_unparsed = builder
        .build()
        .expect("The ref genome config file cannot be loaded.");
    let config: RefGenomeConfig = config_unparsed
        .try_deserialize()
        .expect("The ref genome config file is invalid.");
    config
}

fn random_ids(max: u32, prob: f64) -> Vec<u32> {
    let mut ids = Vec::new();
    for i in 0..max {
        let rand_value: f64 = rand::random();
        if rand_value < prob {
            ids.push(i);
        }
    }
    ids
}
