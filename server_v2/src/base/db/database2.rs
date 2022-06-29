use crate::base::db::{FormattedMutationCount, MutPosSize, Mutation, SeqPosColumn};
use crate::{DatabaseConfig, MutationStore};
use chrono::Local;
use postgres;
use postgres::{Client, NoTls};
use postgres_cursor::Cursor;
use std::collections::HashMap;

const GENES: &'static [&'static str] = &[
    "E", "M", "N", "ORF1a", "ORF1b", "ORF3a", "ORF6", "ORF7a", "ORF7b", "ORF8", "ORF9b", "S",
];

pub fn get_db_client(config: &DatabaseConfig) -> Client {
    let mut db_config = postgres::config::Config::new();
    db_config
        .host(&config.host)
        .port(config.port)
        .user(&config.username)
        .password(&config.password)
        .dbname(&config.dbname);
    let mut client = db_config.connect(NoTls).expect("Database connection failed");
    client
        .execute(&format!("set search_path to '{}'", &config.schema), &[])
        .expect("The search_path of the database could not be changed.");
    client
}

pub struct Database2 {
    pub size: u32,
    nuc_mutation_store: MutationStore,
    aa_mutation_stores: HashMap<String, MutationStore>,
}

impl Database2 {
    pub fn load(config: &DatabaseConfig) -> Database2 {
        let mut client = get_db_client(config);

        // Load size
        let length_sql = "select count(*) from y_main_metadata;";
        let size = client
            .query(length_sql, &[])
            .unwrap()
            .get(0)
            .unwrap()
            .get::<usize, i64>(0) as u32;
        println!("{} Number entries: {}", Local::now(), size);

        let mut nuc_pos_columns: Vec<SeqPosColumn> = Vec::with_capacity(29903);
        println!("{} Let's get started", Local::now());
        for position in 1..29904 {
            if position % 100 == 0 {
                println!("{} {}/{}", Local::now(), position / 100, 29903 / 100);
            }
            let columnar_sequence = load_nuc_columnar(&mut client, position, size);
            let nuc_pos_column = SeqPosColumn::from(columnar_sequence);
            nuc_pos_columns.push(nuc_pos_column);
        }

        // Load mutations
        let (nuc_mutation_store, aa_mutation_stores) = load_mutations(&mut client, size);

        // Finished
        Database2 {
            size,
            nuc_mutation_store,
            aa_mutation_stores,
        }
    }

    pub fn nuc_muts(&self, ids: &Vec<u32>) -> Vec<FormattedMutationCount> {
        self.nuc_mutation_store
            .count_mutations(ids)
            .iter()
            .filter(|x| x.proportion >= 0.05)
            .map(|x| FormattedMutationCount {
                mutation: x.mutation.to_string(),
                proportion: x.proportion,
                count: x.count,
            })
            .collect()
    }
}

fn load_mutations(client: &mut Client, size: u32) -> (MutationStore, HashMap<String, MutationStore>) {
    let mut nuc_mutation_store = MutationStore::with_capacity(size);
    let mut aa_mutation_stores = HashMap::new();
    for gene in GENES {
        aa_mutation_stores.insert(gene.to_string(), MutationStore::with_capacity(size));
    }

    let sequence_sql = "
        select
          id,
          aa_mutations,
          coalesce(aa_unknowns, '') as aa_unknowns,
          nuc_substitutions,
          nuc_deletions,
          coalesce(nuc_unknowns, '') as nuc_unknowns
        from y_main_sequence
        order by id
    ";
    let mut cursor = Cursor::build(client)
        .batch_size(500000)
        .query(sequence_sql)
        .finalize()
        .expect("cursor creation succeeded");

    let mut i = 0;
    for result in &mut cursor {
        let rows = result.unwrap();
        for row in &rows {
            if i % 10000 == 0 {
                println!("{} {}/{}", Local::now(), i / 10000, size / 10000);
            }
            i += 1;
            // Nuc mutations
            let nuc_substitutions: String = row.get("nuc_substitutions");
            let nuc_deletions: String = row.get("nuc_deletions");
            let nuc_unknowns_string: String = row.get("nuc_unknowns");
            let nuc_mutations: Vec<Mutation> = nuc_substitutions
                .split(",")
                .chain(nuc_deletions.split(","))
                .filter(|x| !x.is_empty())
                .map(|x| x.parse().unwrap())
                .collect();
            let nuc_unknowns: Vec<&str> = nuc_unknowns_string.split(",").filter(|x| !x.is_empty()).collect();
            nuc_mutation_store.push(&nuc_mutations, &nuc_unknowns);
            // AA mutations
            let aa_mutations: String = row.get("aa_mutations");
            let aa_unknowns: String = row.get("aa_unknowns");
            let mut aa_mutations_per_gene: HashMap<String, Vec<Mutation>> = HashMap::new();
            let mut aa_unknowns_per_gene: HashMap<String, Vec<&str>> = HashMap::new();
            for gene in GENES {
                aa_mutations_per_gene.insert(gene.to_string(), Vec::new());
                aa_unknowns_per_gene.insert(gene.to_string(), Vec::new());
            }
            aa_mutations.split(",").filter(|x| !x.is_empty()).for_each(|x| {
                let parts: Vec<&str> = x.split(":").collect();
                let mutation: Mutation = parts[1].parse().unwrap();
                aa_mutations_per_gene.get_mut(parts[0]).unwrap().push(mutation);
            });
            aa_unknowns.split(",").filter(|x| !x.is_empty()).for_each(|x| {
                let parts: Vec<&str> = x.split(":").collect();
                aa_unknowns_per_gene.get_mut(parts[0]).unwrap().push(parts[1]);
            });
            for (gene, aa_mutation_store) in &mut aa_mutation_stores {
                aa_mutation_store.push(
                    aa_mutations_per_gene.get(gene).unwrap(),
                    aa_unknowns_per_gene.get(gene).unwrap(),
                );
            }
        }
    }

    (nuc_mutation_store, aa_mutation_stores)
}

fn load_nuc_columnar(client: &mut Client, position: MutPosSize, size: u32) -> Vec<char> {
    let sql = "
        select data_compressed
        from y_main_sequence_columnar
        where position = $1;
    ";
    let rows = client.query(sql, &[&(position as i32)]).unwrap();
    let row = rows
        .get(0)
        .expect(&format!("No nuc columnar data available at position {}", position));
    let compressed_bytes: Vec<u8> = row.get("data_compressed");
    let decompressed_bytes = zstd::bulk::decompress(&*compressed_bytes, size as usize).unwrap();
    let decompressed: Vec<char> = decompressed_bytes.iter().map(|b| *b as char).collect();
    decompressed
}
