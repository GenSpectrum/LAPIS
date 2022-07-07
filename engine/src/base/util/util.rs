use chrono::{Duration, NaiveDate};
use flate2::read::GzDecoder;
use lzma::LzmaReader;
use md5::{Digest, Md5};
use std::collections::HashMap;
use std::error::Error;
use std::ffi::OsStr;
use std::fs::File;
use std::io::{BufReader, Read};
use std::path::Path;

pub fn parse_date(s: &str) -> Option<NaiveDate> {
    NaiveDate::parse_from_str(s, "%Y-%m-%d").ok()
}

pub fn encode_date_as_int(date: &NaiveDate) -> i32 {
    let x = *date - NaiveDate::from_ymd(1970, 1, 1);
    x.num_days() as i32
}

pub fn decode_date_from_int(i: &i32) -> NaiveDate {
    NaiveDate::from_ymd(1970, 1, 1) + Duration::days(*i as i64)
}

/// Opens a file and decompresses the content if the path ends with ".xz" or ".gz"; otherwise just
/// opens the file
pub fn open_file_with_guessed_compression(path: &Path) -> Result<Box<dyn Read>, Box<dyn Error>> {
    let file = File::open(path)?;
    let buffered = BufReader::new(file);

    let extension = path.extension();
    // Decompress if we have a known ending associated with a compressed archive
    if let Some(extension) = extension {
        if extension == OsStr::new("xz") {
            return Ok(Box::new(LzmaReader::new_decompressor(BufReader::new(buffered))?));
        } else if extension == OsStr::new("gz") {
            return Ok(Box::new(GzDecoder::new(BufReader::new(buffered))));
        }
    }
    // Return the file without decompression
    Ok(Box::new(buffered))
}

/// Serializes the value to bytes and then hashes it
pub fn hash_md5<T: ?Sized>(value: &T) -> Result<String, bincode::Error>
where
    T: serde::Serialize,
{
    let encoded: Vec<u8> = bincode::serialize(&value)?;
    let hash = Md5::digest(encoded);
    let mut buf = [0u8; 32];
    Ok(base16ct::lower::encode_str(&hash, &mut buf).unwrap().to_string())
}

/// Turns a hash map to a key-sorted vector and then hashes it
pub fn hash_md5_for_hash_map<K, V>(value: &HashMap<K, V>) -> Result<String, bincode::Error>
where
    K: Ord + Clone + serde::Serialize,
    V: Clone + serde::Serialize,
{
    let mut ls: Vec<(K, V)> = (*value).clone().into_iter().collect();
    ls.sort_by(|(k1, _), (k2, _)| k1.cmp(k2));
    hash_md5(&ls)
}
