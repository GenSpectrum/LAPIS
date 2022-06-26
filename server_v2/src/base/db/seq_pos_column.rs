use roaring::RoaringBitmap;
use std::collections::HashMap;

pub struct SeqPosColumn {
    data: HashMap<char, RoaringBitmap>,
}

impl SeqPosColumn {
    pub fn from(columnar_sequence: Vec<char>) -> Self {
        let mut data: HashMap<char, RoaringBitmap> = HashMap::new();
        for (i, x) in columnar_sequence.iter().enumerate() {
            let map = data
                .entry(x.clone())
                .or_insert_with(|| RoaringBitmap::new());
            map.insert(i as u32);
        }

        for (x, map) in &data {
            println!("{}: {}, {}", x, map.len(), map.serialized_size());
        }
        println!("---");

        SeqPosColumn { data }
    }
}

// pub struct SeqPosColumn2 {
//     data: HashMap<char, Bitmap>
// }
//
// impl SeqPosColumn2 {
//     pub fn from(columnar_sequence: Vec<char>) -> Self {
//         let mut data: HashMap<char, Bitmap> = HashMap::new();
//         for (i, x) in columnar_sequence.iter().enumerate() {
//             let map = data.entry(x.clone()).or_insert_with(|| Bitmap::create());
//             map.add(i as u32);
//         }
//         for (_, map) in &mut data {
//             map.run_optimize();
//         }
//         SeqPosColumn2 { data }
//     }
// }
