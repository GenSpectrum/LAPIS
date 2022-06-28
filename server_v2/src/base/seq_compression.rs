pub struct SeqCompressor {
    compressor: zstd::bulk::Compressor<'static>,
    decompressor: zstd::bulk::Decompressor<'static>,
    dict: Option<Vec<u8>>,
}

impl SeqCompressor {
    /// Creates a compressor without a pre-defined dictionary
    pub fn new() -> Self {
        SeqCompressor {
            compressor: zstd::bulk::Compressor::new(3).unwrap(),
            decompressor: zstd::bulk::Decompressor::new().unwrap(),
            dict: None,
        }
    }

    pub fn with_dict(dict: &[u8]) -> Self {
        SeqCompressor {
            compressor: zstd::bulk::Compressor::with_dictionary(3, dict).unwrap(),
            decompressor: zstd::bulk::Decompressor::with_dictionary(dict).unwrap(),
            dict: Some(dict.to_vec()),
        }
    }

    pub fn compress_bytes(&mut self, bytes: &[u8]) -> Vec<u8> {
        self.compressor.compress(&bytes).unwrap()
    }

    pub fn decompress(&mut self, compressed: &[u8]) -> Vec<u8> {
        let size = zstd::bulk::Decompressor::upper_bound(compressed)
            .expect("The size of the compressed data is unknown.");
        let decompressed_bytes = self.decompressor.decompress(compressed, size).unwrap();
        decompressed_bytes
    }
}

impl Clone for SeqCompressor {
    fn clone(&self) -> Self {
        match &self.dict {
            None => SeqCompressor::new(),
            Some(dict) => SeqCompressor::with_dict(dict),
        }
    }
}
