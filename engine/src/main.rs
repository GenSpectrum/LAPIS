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
use clap::{Parser, Subcommand};
use config::{Config, File, FileFormat};
use std::path::PathBuf;
use std::sync::Arc;

#[derive(Parser)]
#[clap(author, version, about)]
struct Cli {
    #[clap(long, parse(from_os_str))]
    /// Path to the directory with the config files
    config_dir: std::path::PathBuf,
    #[clap(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Generates the PostgreSQL database tables
    Init {},
    /// Imports data
    Import {
        #[clap(parse(from_os_str))]
        /// Path to the directory with the source data
        data_dir: PathBuf,
    },
    /// Starts the server
    Server {
        #[clap(long, value_parser, default_value_t = 8080)]
        /// The port that the HTTP server should use
        port: u16,
    },
}

fn main() {
    let cli: Cli = Cli::parse();
    let config = read_config(&cli.config_dir);

    println!("{} Welcome", Local::now());
    match cli.command {
        Commands::Init {} => {
            db::generate_db_tables(&config.database, &config.schema);
        }
        Commands::Import { data_dir } => {
            let ref_genome_config = read_ref_genome_config(&cli.config_dir);
            let mut nuc_seq_compressor = SeqCompressor::with_dict(ref_genome_config.sequence.as_bytes());
            base::proc::load_source_data(
                &data_dir,
                &config.schema,
                &config.database,
                &ref_genome_config,
                &mut nuc_seq_compressor,
            );
            base::proc::source_to_main(&config.schema, &config.database, &mut nuc_seq_compressor);
        }
        Commands::Server { port } => {
            let db = Arc::new(Database::load(&config.schema, &config.database));
            server::main(db, port).unwrap();
        }
    }

    println!("Done");
}

fn read_config(config_dir: &PathBuf) -> ProgramConfig {
    let path = config_dir.join("config.yml").into_os_string().into_string().unwrap();
    let builder = Config::builder().add_source(File::new(&path, FileFormat::Yaml));
    let config_unparsed = builder.build().expect("The config file cannot be loaded.");
    let config: ProgramConfig = config_unparsed.try_deserialize().expect("The config file is invalid.");
    config
}

fn read_ref_genome_config(config_dir: &PathBuf) -> RefGenomeConfig {
    let path = config_dir
        .join("config_ref_genome.yml")
        .into_os_string()
        .into_string()
        .unwrap();
    let builder = Config::builder().add_source(File::new(&path, FileFormat::Yaml));
    let config_unparsed = builder.build().expect("The ref genome config file cannot be loaded.");
    let config: RefGenomeConfig = config_unparsed
        .try_deserialize()
        .expect("The ref genome config file is invalid.");
    config
}
