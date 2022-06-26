pub struct SeqCompressor {
    compressor: zstd::bulk::Compressor<'static>,
    decompressor: zstd::bulk::Decompressor<'static>,
}

impl SeqCompressor {
    /// Creates a compressor without a pre-defined dictionary
    pub fn new() -> Self {
        SeqCompressor {
            compressor: zstd::bulk::Compressor::new(3).unwrap(),
            decompressor: zstd::bulk::Decompressor::new().unwrap(),
        }
    }

    pub fn with_dict(dict: &[u8]) -> Self {
        SeqCompressor {
            compressor: zstd::bulk::Compressor::with_dictionary(3, dict).unwrap(),
            decompressor: zstd::bulk::Decompressor::with_dictionary(dict).unwrap(),
        }
    }

    pub fn compress_bytes(&mut self, bytes: &[u8]) -> Vec<u8> {
        self.compressor.compress(&bytes).unwrap()
    }

    pub fn decompress(&mut self, compressed: &[u8]) -> Vec<char> {
        let decompressed_bytes = self
            .decompressor
            .decompress(compressed, compressed.len())
            .unwrap();
        let decompressed: Vec<char> = decompressed_bytes.iter().map(|b| *b as char).collect();
        decompressed
    }
}
