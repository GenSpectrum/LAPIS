use crate::base::constants::NucCode;
use crate::base::constants::NucCode::*;
use bio::data_structures::suffix_array::SuffixArray;
use std::hash::Hash;

#[derive(Clone, Hash, Eq, PartialEq)]
pub struct NucMutation {
    pub position: usize,
    pub to: NucCode,
}

pub fn find_nuc_mutations(mut aligned_seq: Vec<NucCode>, reference: &Vec<NucCode>) -> Vec<NucMutation> {
    // Masking leading and tailing deletions because they are often actually unknowns but appear here as
    // deletions due to aligning.
    for x in aligned_seq.iter_mut() {
        if *x != GAP {
            break;
        }
        *x = N;
    }
    for x in aligned_seq.iter_mut().rev() {
        if *x != GAP {
            break;
        }
        *x = N;
    }

    let mut mutations = Vec::new();
    for (i, ref_code) in reference.iter().enumerate() {
        let seq_code = aligned_seq.get(i).unwrap();
        if *seq_code != C && *seq_code != T && *seq_code != A && *seq_code != G && *seq_code != GAP {
            continue;
        }
        if *seq_code != *ref_code {
            mutations.push(NucMutation {
                position: i + 1,
                to: seq_code.clone(),
            });
        }
    }

    mutations
}

/// Everything that is not A, T, C, G or - is considered as unknown.
pub fn find_nuc_unknowns(mut aligned_seq: Vec<NucCode>) -> Vec<usize> {
    // Masking leading and tailing deletions because they are often actually unknowns but appear here as
    // deletions due to aligning.
    for x in aligned_seq.iter_mut() {
        if *x != GAP {
            break;
        }
        *x = N;
    }
    for x in aligned_seq.iter_mut().rev() {
        if *x != GAP {
            break;
        }
        *x = N;
    }

    let mut positions = Vec::new();
    for (i, seq_code) in aligned_seq.iter().enumerate() {
        if *seq_code != C && *seq_code != T && *seq_code != A && *seq_code != G && *seq_code != GAP {
            positions.push(i + 1);
        }
    }

    positions
}

/// This takes a list of position integers and introduces range representations if appropriate. Example:
/// 1,5,6,7,8,20 will be transformed to 1,5-8,20
pub fn compress_positions_as_strings(positions: &Vec<usize>) -> Vec<String> {
    if positions.is_empty() {
        return Vec::new();
    }

    let mut result = Vec::new();
    let mut range_start = positions.get(0).unwrap();
    let mut range_end = range_start;

    for pos in positions {
        if *pos == range_end {
            // This is the initial case and there is nothing to do as range_start and range_end
            // are already initialized above.
        } else if *pos == range_end + 1 {
            // If the range is being continued
            range_end = *pos;
        } else {
            // If the range ended
            if range_end - range_start > 1 {
                // If there is at least one number in between
                result.push(format!("{}-{}", range_start, range_end));
            } else if range_end - range_start > 0 {
                // If there are two different numbers
                result.push(range_start.to_string());
                result.push(range_end.to_string());
            } else {
                // If there is a single number
                result.push(range_start.to_string());
            }
            range_start = *pos;
            range_end = *pos;
        }
    }
    // Finishing up - same code as above
    if range_end - range_start > 1 {
        // If there is at least one number in between
        result.push(format!("{}-{}", range_start, range_end));
    } else if range_end - range_start > 0 {
        // If there are two different numbers
        result.push(range_start.to_string());
        result.push(range_end.to_string());
    } else {
        // If there is a single number
        result.push(range_start.to_string());
    }

    result
}
