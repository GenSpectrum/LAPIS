use crate::{ConnectionPool, ExecutorService, SeqCompressor};
use chrono::Local;
use md5::{Digest, Md5};
use postgres_cursor::Cursor;
use std::collections::HashMap;
use std::fs::File;
use std::io::{BufRead, BufReader, Write};
use std::sync::{Arc, Mutex};

pub fn hash_uppercase(db_pool: &ConnectionPool, seq_compressor: &mut SeqCompressor) {
    // Load from file
    let mut old_hashes = HashMap::new();
    let file = File::open("/Users/chachen/Downloads/old.tsv").unwrap();
    let reader = BufReader::new(file);
    for line in reader.lines() {
        let line = line.unwrap();
        let (hash, id) = line.split_once("\t").unwrap();
        old_hashes.insert(id.to_string(), hash.to_string());
    }
    let old_hashes_arc = Arc::new(old_hashes);
    println!("{} File loaded", Local::now());

    // Load from DB
    let mut client = db_pool.get();
    let sequence_sql = "
        select
          gisaid_epi_isl,
          seq_original_compressed
        from y_gisaid;
    ";
    let batch_size = 50000;
    let mut cursor = Cursor::build(&mut client)
        .batch_size(batch_size)
        .query(sequence_sql)
        .finalize()
        .expect("cursor creation succeeded");
    let outdated = Arc::new(Mutex::new(Vec::new()));
    let missing = Arc::new(Mutex::new(Vec::new()));
    let mut iteration = 0;
    let mut executor = ExecutorService::new(6);
    for rows in &mut cursor {
        println!(
            "{} Processing {}..{}",
            Local::now(),
            iteration * batch_size,
            (iteration + 1) * batch_size
        );
        iteration += 1;
        let mut batch = Vec::new();
        for row in &rows.unwrap() {
            let id: String = row.get("gisaid_epi_isl");
            let compressed: Option<Vec<u8>> = row.get("seq_original_compressed");
            if let None = compressed {
                continue;
            }
            batch.push((id, compressed.unwrap()));
        }
        let mut seq_compressor = seq_compressor.clone();
        let outdated = outdated.clone();
        let missing = missing.clone();
        let old_hashes_arc = old_hashes_arc.clone();
        let task = move |worker| {
            let batch = batch;
            for (id, compressed) in &batch {
                let seq = String::from_utf8(seq_compressor.decompress(compressed)).unwrap();
                let mut cleaned = seq.to_uppercase();
                cleaned.retain(|c| !c.is_ascii_whitespace());

                let hash = Md5::digest(cleaned);
                let mut buf = [0u8; 32];
                let hash_str = base16ct::lower::encode_str(&hash, &mut buf).unwrap();
                match old_hashes_arc.get(id) {
                    None => {
                        missing.lock().unwrap().push(id.to_string());
                    }
                    Some(old_hash) => {
                        if old_hash != hash_str {
                            {
                                outdated.lock().unwrap().push(id.to_string());
                            }
                        }
                    }
                }
            }
            println!("{} Worker {} finished a batch", Local::now(), worker);
        };
        executor.submit_task(task);
    }
    executor.close();
    executor.join();

    // Write outdated/missing entries to file
    File::create("missing.txt")
        .unwrap()
        .write_all(missing.lock().unwrap().join("\n").as_bytes())
        .unwrap();
    File::create("outdated.txt")
        .unwrap()
        .write_all(outdated.lock().unwrap().join("\n").as_bytes())
        .unwrap();
}
