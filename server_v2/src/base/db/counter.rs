use std::collections::HashMap;
use std::hash::Hash;

pub struct Counter<T: Hash + Eq + Clone> {
    pub data: HashMap<T, u32>,
}

impl<T: Hash + Eq + Clone> Counter<T> {
    pub fn new() -> Counter<T> {
        Counter {
            data: HashMap::new()
        }
    }

    pub fn update(&mut self, list: &Vec<T>) {
        for item in list {
            let new_count = self.data.get(item).unwrap_or(&0) + 1;
            self.data.insert(item.clone(), new_count);
        }
    }
}
