use crate::{ExecutorService, SeqCompressor};
use std::cmp;

/// Given a list of k strings of length n (aligned sequences), this class produces n strings of
/// length k. The first string contains the first character of the input strings, etc.
///
/// Example:
/// * Input: ["AAT", "ATT"]
/// * Output: ["AA", "AT", "TT"]
pub struct SequenceRowToColumnTransformer {
    number_workers: usize,

    /// This value defines how many positions should be processed at the same time, i.e., how many
    /// strings of length k has to be held in memory. This is the core value to balance the needed
    /// RAM, CPU and wall-clock time.
    position_range_size: usize,

    /// This is the number of sequences that a worker will process per batch. It usually does not
    /// have a large effect on the performance.
    batch_size: usize,
}

impl SequenceRowToColumnTransformer {
    pub fn new(number_workers: usize, position_range_size: usize) -> Self {
        SequenceRowToColumnTransformer {
            number_workers,
            position_range_size,
            batch_size: 30000,
        }
    }

    /// # Arguments
    ///
    /// * `compressed_sequences` - The compressed input sequences
    /// * `decompressor` - A SeqCompressor to decompress the input strings
    /// * `compressor` - A SeqCompressor to compress the transformed strings
    /// * `consume` - A function that takes the position of the first entry (index starts with 1)
    /// in the result set as the first argument and a list of transformed and compressed columnar
    /// strings as the second argument.
    /// * `unknown_code` - The character that should be inserted if no sequence is available. For
    /// nucleotide sequences, this is usually `N`. For amino acid sequences, it's usually `X`.
    pub fn transform<T: 'static + Send + Fn(usize, Vec<Vec<u8>>) + Clone>(
        &self,
        compressed_sequences: &Vec<Option<Vec<u8>>>,
        decompressor: &SeqCompressor,
        compressor: &SeqCompressor,
        consume: T,
        unknown_code: u8,
    ) {
        if compressed_sequences.is_empty() {
            return;
        }
        let sequence_length = match compressed_sequences.get(0).unwrap() {
            None => panic!(),
            Some(seq) => decompressor.clone().decompress(seq).len(),
        };
        let number_iterations = (sequence_length as f32 / self.position_range_size as f32).ceil() as usize;
        let number_tasks_per_iteration = (compressed_sequences.len() as f32 / self.batch_size as f32).ceil() as usize;

        // Create a thread pool with a fixed number of threads
        let mut executor = ExecutorService::new(self.number_workers);

        // Do the work
        for iteration in 0..number_iterations {
            let start_pos = self.position_range_size * iteration;
            let end_pos = cmp::min(self.position_range_size * (iteration + 1), sequence_length);
            let count_pos = end_pos - start_pos;
            let mut transformed_data = vec![vec![b'?'; compressed_sequences.len()]; count_pos];
            let shared_data = SharedData {
                compressed_sequences: compressed_sequences as *const Vec<Option<Vec<u8>>>,
                transformed_data: &mut transformed_data as *mut Vec<Vec<u8>>,
            };

            // Decompress the sequences and transform
            for task_index in 0..number_tasks_per_iteration {
                let start_seq = self.batch_size * task_index;
                let end_seq = cmp::min(self.batch_size * (task_index + 1), compressed_sequences.len());
                let shared_data = shared_data.clone();
                let mut decompressor = decompressor.clone();
                let task = move |_: usize| unsafe {
                    let shared_data = shared_data;
                    let compressed_sequences = &*shared_data.compressed_sequences;
                    let transformed_data = &mut *shared_data.transformed_data;
                    for seq_index in start_seq..end_seq {
                        let compressed = compressed_sequences.get(seq_index).unwrap();
                        match compressed {
                            None => {
                                for i in start_pos..end_pos {
                                    *(transformed_data
                                        .get_mut(i - start_pos)
                                        .unwrap()
                                        .get_mut(seq_index)
                                        .unwrap()) = unknown_code;
                                }
                            }
                            Some(seq) => {
                                let decompressed = decompressor.decompress(seq);
                                for i in start_pos..end_pos {
                                    *(transformed_data
                                        .get_mut(i - start_pos)
                                        .unwrap()
                                        .get_mut(seq_index)
                                        .unwrap()) = decompressed.get(i).unwrap().clone();
                                }
                            }
                        }
                    }
                };
                executor.submit_task(task);
            }
            executor.join();

            // Transform char arrays to string, compress them and insert
            // This will be done in parallel again.
            let finalization_batch_size = 2000;
            let number_finalization_tasks = (count_pos as f32 / finalization_batch_size as f32).ceil() as usize;
            println!("number_finalization_tasks: {}", number_finalization_tasks);
            for finalization_index in 0..number_finalization_tasks {
                let finalization_pos_start = start_pos + finalization_batch_size * finalization_index;
                let finalization_pos_end =
                    cmp::min(start_pos + finalization_batch_size * (finalization_index + 1), end_pos);
                let shared_data = shared_data.clone();
                let mut compressor = compressor.clone();

                let consume = consume.clone();
                let task = move |_: usize| unsafe {
                    let shared_data = shared_data;
                    let transformed_data = &*shared_data.transformed_data;
                    let mut results = Vec::new();
                    for pos_index in finalization_pos_start..finalization_pos_end {
                        let transformed = transformed_data.get(pos_index - start_pos).unwrap();
                        let compressed = compressor.compress_bytes(transformed);
                        results.push(compressed);
                    }
                    consume(finalization_pos_start + 1, results);
                };
                println!("Insert finalization task");
                executor.submit_task(task);
            }
            executor.join();
        }

        // Stop the threads
        executor.close();
    }
}

#[derive(Clone)]
struct SharedData {
    compressed_sequences: *const Vec<Option<Vec<u8>>>,
    transformed_data: *mut Vec<Vec<u8>>,
}

unsafe impl Send for SharedData {}

#[cfg(test)]
mod tests {
    use crate::{SeqCompressor, SequenceRowToColumnTransformer};
    use std::sync::{Arc, Mutex};

    #[test]
    fn test() {
        let transformer = SequenceRowToColumnTransformer::new(2, 5);
        let mut compressor = SeqCompressor::new();
        let sequences = vec!["ABCDEFGHIJKLMNOP", "abcdefghijklmnop", "0123456789abcdef"];
        let compressed_sequences: Vec<Option<Vec<u8>>> = sequences
            .iter()
            .map(|s| Some(compressor.compress_bytes(s.as_bytes())))
            .collect();
        let results = Arc::new(Mutex::new(Vec::<(usize, Vec<Vec<u8>>)>::new()));
        let results_ref_copy = results.clone();
        let consume = move |pos_offset, transformed_seqs| {
            let mut y = results_ref_copy.lock().unwrap();
            y.push((pos_offset, transformed_seqs));
        };
        transformer.transform(&compressed_sequences, &compressor, &compressor, consume, b'!');
        let mut x = results.lock().unwrap();
        x.sort_by_key(|x| x.0);
        let y: Vec<_> = x
            .iter()
            .map(|x| &x.1)
            .flatten()
            .map(|x| String::from_utf8(compressor.decompress(x)).unwrap())
            .collect();
        assert_eq!(
            y,
            vec![
                "Aa0", "Bb1", "Cc2", "Dd3", "Ee4", "Ff5", "Gg6", "Hh7", "Ii8", "Jj9", "Kka", "Llb", "Mmc", "Nnd",
                "Ooe", "Ppf"
            ]
        );
    }
}
