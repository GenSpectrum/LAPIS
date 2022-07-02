use crate::base::db::{BiDict, Counter};
use crate::mutation;
use std::sync::Arc;

// Basic public structs

pub type MutPosSize = usize;

pub type Mutation = mutation::NucMutation;

// MutationStore

pub struct MutationStore {
    data: Vec<InternalEntry>,
    mutation_dict: BiDict<Mutation>,
    max_position: MutPosSize,
}

#[derive(Clone, PartialEq)]
pub struct MutationCount {
    pub mutation: Arc<Mutation>,
    pub count: u32,
    pub proportion: f64,
}

impl MutationStore {
    pub fn with_capacity(capacity: u32) -> Self {
        MutationStore {
            data: Vec::with_capacity(capacity as usize),
            mutation_dict: BiDict::new(),
            max_position: 0,
        }
    }

    pub fn push(&mut self, mutations: &Vec<Mutation>, unknowns_compressed_positions: &Vec<&str>) {
        // Encode the mutations using the MutationDict
        let mut mutation_ids: Vec<u32> = Vec::with_capacity(mutations.len());
        for mutation in mutations.iter() {
            mutation_ids.push(self.mutation_dict.value_to_id(mutation.clone()));
            if mutation.position > self.max_position {
                self.max_position = mutation.position;
            }
        }
        // Parse the compressed unknown position strings
        let mut unknowns: Vec<UnknownPosition> = Vec::new();
        for s in unknowns_compressed_positions {
            let parts: Vec<&str> = s.split("-").collect();
            if parts.len() == 1 {
                // It's a single value
                let position: MutPosSize = parts[0]
                    .parse()
                    .expect(&format!("Unexpected compressed unknowns string: {}", s));
                unknowns.push(UnknownPosition {
                    position,
                    is_start_range: false,
                });
                if position > self.max_position {
                    self.max_position = position;
                }
            } else {
                // It's a range
                let range_start: MutPosSize = parts[0]
                    .parse()
                    .expect(&format!("Unexpected compressed unknowns string: {}", s));
                let range_end: MutPosSize = parts[1]
                    .parse()
                    .expect(&format!("Unexpected compressed unknowns string: {}", s));
                unknowns.push(UnknownPosition {
                    position: range_start,
                    is_start_range: true,
                });
                unknowns.push(UnknownPosition {
                    position: range_end,
                    is_start_range: false,
                });
                if range_end > self.max_position {
                    self.max_position = range_end;
                }
            }
        }
        // Create and store entry
        self.data.push(InternalEntry { mutation_ids, unknowns })
    }

    pub fn count_mutations(&self, ids: &Vec<u32>) -> Vec<MutationCount> {
        let mut mutation_id_counts: Counter<u32> = Counter::new();
        // To avoid too many annoying add-1 and minus-1 operations, we will leave out the first
        // position in the array so that the "position" corresponds to the index.
        let mut unknown_counts: Vec<u32> = vec![0; (self.max_position as usize) + 1];
        for id in ids {
            let entry = &self.data[*id as usize];
            // Count mutations
            mutation_id_counts.update(&entry.mutation_ids);
            // Count unknowns
            let mut i = 0;
            while i < entry.unknowns.len() {
                let unknown = &entry.unknowns[i];
                if !unknown.is_start_range {
                    *unknown_counts.get_mut(unknown.position as usize).unwrap() += 1;
                } else {
                    let start = unknown.position;
                    let end = entry.unknowns[i + 1].position;
                    for position in start..end + 1 {
                        *unknown_counts.get_mut(position as usize).unwrap() += 1;
                    }
                    i += 1; // Skip the end range value
                }
                i += 1;
            }
        }
        // Translate the mutations back and calculate the proportions:
        //   proportion = count / (total number of entries - number of unknowns)
        let total_entries = ids.len() as u32;
        let mut mutation_counts: Vec<MutationCount> = Vec::with_capacity(mutation_id_counts.data.len());
        for (mutation_id, count) in mutation_id_counts.data {
            let mutation = self.mutation_dict.id_to_value(mutation_id);
            let denominator = total_entries - unknown_counts[mutation.position as usize];
            let proportion = (count as f64) / (denominator as f64);
            mutation_counts.push(MutationCount {
                mutation,
                count,
                proportion,
            });
        }
        mutation_counts
    }
}

// Internal

struct InternalEntry {
    mutation_ids: Vec<u32>,
    unknowns: Vec<UnknownPosition>,
}

struct UnknownPosition {
    position: MutPosSize,
    is_start_range: bool,
}
