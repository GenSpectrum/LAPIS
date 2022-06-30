use crate::base::constants::NucCode;
use crate::db::Column;
use crate::Database;
use chrono::NaiveDate;
use serde_json::Value;

pub trait Filter {
    fn evaluate(&self, database: &Database) -> Vec<bool>;
}

pub fn from_json(json: &str) -> Option<Box<dyn Filter>> {
    let value: Value = serde_json::from_str(json).ok()?;
    from_json_value(&value)
}

pub fn from_json_value(json: &Value) -> Option<Box<dyn Filter>> {
    if let Value::Object(obj) = json {
        let op_type = obj.get("type")?;
        if let Value::String(op_type) = op_type {
            return match op_type.as_str() {
                "And" => Some(Box::new(And::from_json(json)?)),
                "Or" => Some(Box::new(Or::from_json(json)?)),
                "Neg" => Some(Box::new(Neg::from_json(json)?)),
                "StrEq" => Some(Box::new(StrEq::from_json(json)?)),
                "DateBetw" => Some(Box::new(DateBetw::from_json(json)?)),
                "NucEq" => Some(Box::new(NucEq::from_json(json)?)),
                "NOf" => Some(Box::new(NOf::from_json(json)?)),
                _ => None,
            };
        }
    }
    None
}

pub struct And {
    pub children: Vec<Box<dyn Filter>>,
}

impl And {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let children = obj.get("children")?;
            let mut children_parsed = Vec::new();
            if let Value::Array(children) = children {
                for child in children {
                    let child_parsed = from_json_value(child)?;
                    children_parsed.push(child_parsed);
                }
                return Some(And {
                    children: children_parsed,
                });
            }
        }
        None
    }
}

impl Filter for And {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.children.is_empty() {
            return vec![true; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> = self.children.iter().map(|c| c.evaluate(database)).collect();
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
    pub children: Vec<Box<dyn Filter>>,
}

impl Or {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let children = obj.get("children")?;
            let mut children_parsed = Vec::new();
            if let Value::Array(children) = children {
                for child in children {
                    let child_parsed = from_json_value(child)?;
                    children_parsed.push(child_parsed);
                }
                return Some(Or {
                    children: children_parsed,
                });
            }
        }
        None
    }
}

impl Filter for Or {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.children.is_empty() {
            return vec![false; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> = self.children.iter().map(|c| c.evaluate(database)).collect();
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
    pub child: Box<dyn Filter>,
}

impl Neg {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let child = obj.get("child")?;
            let child_parsed = from_json_value(child)?;
            return Some(Neg { child: child_parsed });
        }
        None
    }
}

impl Filter for Neg {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        self.child.evaluate(database).iter().map(|b| !b).collect()
    }
}

pub struct StrEq {
    pub column: String,
    pub value: String,
}

impl StrEq {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let column = obj.get("column")?;
            let value = obj.get("value")?;
            if let Value::String(column) = column {
                if let Value::String(value) = value {
                    return Some(StrEq {
                        column: column.clone(),
                        value: value.clone(),
                    });
                }
            }
        }
        None
    }
}

impl Filter for StrEq {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        let data = database
            .metadata
            .get(&*self.column)
            .expect(&*format!("Metadata column {} is missing", self.column));
        match data {
            Column::Str(data) => data
                .iter()
                .map(|s| if let Some(s) = s { *s == self.value } else { false })
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

impl DateBetw {
    pub fn from_json(json: &Value) -> Option<Self> {
        todo!()
    }
}

impl Filter for DateBetw {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        todo!()
    }
}

pub struct NucEq {
    pub position: u32,
    pub value: NucCode,
}

impl NucEq {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let position = obj.get("position")?;
            let value = obj.get("value")?;
            if let Value::Number(position) = position {
                let position = position.as_u64()?;
                if let Value::String(value) = value {
                    let value_bytes = value.as_bytes();
                    if value_bytes.len() == 1 {
                        let nuc_code = NucCode::from_byte(value_bytes[0])?;
                        return Some(NucEq {
                            position: position as u32,
                            value: nuc_code,
                        });
                    }
                }
            }
        }
        None
    }
}

impl Filter for NucEq {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        let data = database.load_nuc_column(self.position);
        data.iter().map(|x| *x == self.value).collect()
    }
}

pub struct NOf {
    pub n: u32,
    pub exactly: bool,
    pub children: Vec<Box<dyn Filter>>,
}

impl NOf {
    pub fn from_json(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            let n = obj.get("n")?;
            let exactly = obj.get("exactly")?;
            let children = obj.get("children")?;
            let mut children_parsed = Vec::new();
            if let Value::Array(children) = children {
                for child in children {
                    let child_parsed = from_json_value(child)?;
                    children_parsed.push(child_parsed);
                }
                if let Value::Number(n) = n {
                    let n = n.as_u64()?;
                    if let Value::Bool(exactly) = exactly {
                        return Some(NOf {
                            n: n as u32,
                            exactly: *exactly,
                            children: children_parsed,
                        });
                    }
                }
            }
        }
        None
    }
}

impl Filter for NOf {
    fn evaluate(&self, database: &Database) -> Vec<bool> {
        if self.n as usize > self.children.len() {
            return vec![false; database.number_entries];
        }
        if self.n == 0 {
            return vec![true; database.number_entries];
        }
        let children_evaluated: Vec<Vec<bool>> = self.children.iter().map(|c| c.evaluate(database)).collect();
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

#[cfg(test)]
mod tests {
    use crate::filters::{from_json, StrEq};

    #[test]
    fn parse_json() {
        let json = r#"
{
  "type": "And",
  "children": [
    {
      "type": "StrEq",
      "column": "country",
      "value": "Switzerland"
    },
    {
      "type": "Or",
      "children": [
        {
          "type": "NucEq",
          "position": 25407,
          "value": "A"
        },
        {
          "type": "Neg",
          "child": {
            "type": "StrEq",
            "column": "clade",
            "value": "hMPXV-1"
          }
        }
      ]
    }
  ]
}
        "#;
        let parsed = from_json(json).unwrap();
        // TODO This test currently does not check for correctness.
        //  I don't know how to make Filter (which is a trait..) comparable.
    }
}
