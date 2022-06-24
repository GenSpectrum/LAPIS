mod base;

use config::{Config, File, FileFormat};
use crate::base::db::{MutationStore,  DatabaseConfig, Database};
use crate::base::db;
use serde::Deserialize;
use chrono::Local;
use std::{thread, time};
use std::path::Path;
use crate::base::ProgramConfig;


fn main() {
    println!("{} Welcome", Local::now());

    let builder = Config::builder()
        .add_source(File::new("config.yml", FileFormat::Yaml));

    let config_unparsed = builder.build().expect("The config file cannot be loaded.");
    let config: ProgramConfig = config_unparsed.try_deserialize()
        .expect("The config file is invalid.");

    // db::generate_db_tables(&config.database, &config.schema);
    base::proc::load_source_data(Path::new("/Users/chachen/polybox/tmp_mpox/data"), &config.schema, &config.database);

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

