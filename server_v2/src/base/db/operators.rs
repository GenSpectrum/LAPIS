use crate::base::constants::NucCode;
use crate::db::Column;
use crate::Database;
use chrono::NaiveDate;

pub trait Operator {
    fn evaluate(&self, database: &Database) -> Vec<bool>;
}

pub struct And {
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for And {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.children.is_empty() {
            return vec![true; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> =
            self.children.iter().map(|c| c.evaluate(database)).collect();
        let mut result = Vec::with_capacity(database.number_entries);
        for i in 0..database.number_entries {
            let mut b = true;
            for col in &children_evaluated {
                if !col.get(i).unwrap() {
                    b = false;
                    break;
                }
            }
            result.push(b);
        }
        result
    }
}

pub struct Or {
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for Or {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.children.is_empty() {
            return vec![false; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> =
            self.children.iter().map(|c| c.evaluate(database)).collect();
        let mut result = Vec::with_capacity(database.number_entries);
        for i in 0..database.number_entries {
            let mut b = false;
            for col in &children_evaluated {
                if *col.get(i).unwrap() {
                    b = true;
                    break;
                }
            }
            result.push(b);
        }
        result
    }
}

pub struct Neg {
    pub child: dyn Operator,
}

impl Operator for Neg {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        self.child.evaluate(database).iter().map(|b| !b).collect()
    }
}

pub struct StrEq {
    pub column: String,
    pub value: String,
}

impl Operator for StrEq {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        let data = database
            .metadata
            .get(&*self.column)
            .expect(&*format!("Metadata column {} is missing", self.column));
        match data {
            Column::Str(data) => data
                .iter()
                .map(|s| {
                    if let Some(s) = s {
                        *s == self.value
                    } else {
                        false
                    }
                })
                .collect(),
            _ => panic!(""),
        }
    }
}

pub struct DateBetw {
    pub column: String,
    pub from: Option<NaiveDate>,
    pub to: Option<NaiveDate>,
}

impl Operator for DateBetw {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        todo!()
    }
}

pub struct NucEq {
    pub position: u32,
    pub value: NucCode,
}

impl Operator for NucEq {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        let data = database.load_nuc_column(self.position);
        data.iter().map(|x| *x == self.value).collect()
    }
}

pub struct NOf {
    pub n: u32,
    pub exactly: bool,
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for NOf {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.n as usize > self.children.len() {
            return vec![false; database.number_entries];
        }
        if self.n == 0 {
            return vec![true; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> =
            self.children.iter().map(|c| c.evaluate(database)).collect();
        let mut result = Vec::with_capacity(database.number_entries);
        for i in 0..database.number_entries {
            let mut number_trues: u32 = 0;
            for col in &children_evaluated {
                if *col.get(i).unwrap() {
                    number_trues += 1;
                }
            }
            if self.exactly {
                result.push(number_trues == self.n);
            } else {
                result.push(number_trues >= self.n);
            }
        }
        result
    }
}

// ------ Temporary examples ------

pub fn ex0() -> StrEq {
    StrEq {
        column: "country".to_string(),
        value: "Switzerland".to_string(),
    }
}

pub fn ex1() -> And {
    And {
        children: vec![
            Box::new(StrEq {
                column: "region".to_string(),
                value: "Europe".to_string(),
            }),
            Box::new(StrEq {
                column: "clade".to_string(),
                value: "hMPXV-1".to_string(),
            }),
        ],
    }
}

pub fn ex2() -> NucEq {
    NucEq {
        position: 25407,
        value: NucCode::A,
    }
}
