use std::collections::HashMap;
use std::hash::Hash;
use std::rc::Rc;

pub struct BiDict<T: Eq + Hash + Clone> {
    entry_to_id_map: HashMap<Rc<T>, u32>,
    values: Vec<Rc<T>>,
    next_id: u32,
}

impl<T: Eq + Hash + Clone> BiDict<T> {
    pub fn new() -> Self {
        BiDict {
            values: Vec::new(),
            entry_to_id_map: HashMap::new(),
            next_id: 0,
        }
    }

    pub fn value_to_id(&mut self, value: T) -> u32 {
        match self.entry_to_id_map.get(&value) {
            Some(&id) => id,
            None => {
                let pointer = Rc::new(value);
                self.entry_to_id_map.insert(pointer.clone(), self.next_id);
                self.values.push(pointer);
                self.next_id += 1;
                self.next_id - 1
            }
        }
    }

    pub fn id_to_value(&self, id: u32) -> Rc<T> {
        self.values[id as usize].clone()
    }
}
