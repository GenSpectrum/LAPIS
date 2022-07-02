use crate::base::constants::NucCode;
use crate::base::util::decode_date_from_int;
use crate::base::DataType;
use crate::db::query::QuerySelect::{Aggregated, Details, NucMutations, NucSequences};
use crate::db::{Column, MutationCount};
use crate::{filters, Database, Filter};
use serde_json::Value;
use std::collections::HashMap;
use std::hash::Hash;

pub struct Query {
    pub select: QuerySelect,
    pub filter: Box<dyn Filter>,
}

impl Query {
    pub fn from_json(json: &str) -> Option<Self> {
        let value: Value = serde_json::from_str(json).ok()?;
        if let Value::Object(obj) = value {
            let filter = filters::from_json_value(obj.get("filter")?)?;
            let query_select = obj.get("select")?;
            if let Value::Object(query_type_obj) = query_select {
                if let Value::String(t) = query_type_obj.get("type")? {
                    let query_select_parsed = match t.as_str() {
                        "aggregated" => Some(Aggregated(AggregatedQuery::from_json_value(query_select)?)),
                        "details" => Some(Details(DetailsQuery::from_json_value(query_select)?)),
                        "nucSequences" => Some(NucSequences(NucSequencesQuery::from_json_value(query_select)?)),
                        "nucMutations" => Some(NucMutations(NucMutationsQuery::from_json_value(query_select)?)),
                        _ => None,
                    }?;
                    return Some(Query {
                        select: query_select_parsed,
                        filter,
                    });
                }
            }
        }
        None
    }
}

/// "Select" is probably not the best name.. The information stored in here corresponds to a mix of
/// SQL's select and group by.
pub enum QuerySelect {
    Aggregated(AggregatedQuery),
    Details(DetailsQuery),
    NucSequences(NucSequencesQuery),
    NucMutations(NucMutationsQuery),
}

pub struct AggregatedQuery {
    pub fields: Vec<String>,
}

impl AggregatedQuery {
    pub fn from_json_value(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            if let Value::Array(fields) = obj.get("fields")? {
                let mut fields_parsed = Vec::new();
                for f in fields {
                    if let Value::String(f) = f {
                        fields_parsed.push(f.to_string());
                    } else {
                        return None;
                    }
                }
                return Some(AggregatedQuery { fields: fields_parsed });
            }
        }
        None
    }

    pub fn evaluate(&self, filtered: &Vec<bool>, database: &Database) -> AggregatedQueryResult {
        let mut counts: Vec<(AggregationKey, u32)> = Vec::new();

        if self.fields.is_empty() {
            let mut count: u32 = 0;
            for b in filtered {
                if *b {
                    count += 1;
                }
            }
            counts.push((AggregationKey::new(), count));
        } else {
            let mut count_map = HashMap::<AggregationKey, u32>::new();
            for (i, b) in filtered.iter().enumerate() {
                if *b {
                    let mut key: AggregationKey = AggregationKey::new();
                    for field in &self.fields {
                        let data = database.metadata.get(field).unwrap();
                        match data {
                            Column::Str(xs) => key.put_str(xs.get(i).unwrap().clone()),
                            Column::Int(xs) => key.put_int(xs.get(i).unwrap().clone()),
                        }
                    }
                    let count = count_map.get_mut(&key);
                    match count {
                        None => {
                            count_map.insert(key, 1);
                        }
                        Some(count) => {
                            *count += 1;
                        }
                    }
                }
            }
            for x in count_map {
                counts.push(x);
            }
        }
        AggregatedQueryResult {
            fields: self.fields.clone(),
            counts,
        }
    }
}

pub struct AggregatedQueryResult {
    pub fields: Vec<String>,
    pub counts: Vec<(AggregationKey, u32)>,
}

impl AggregatedQueryResult {
    pub fn to_json(&self, database: &Database) -> String {
        let json_value = Value::Array(
            self.counts
                .iter()
                .map(|(key, count)| {
                    let mut map = serde_json::Map::<String, Value>::with_capacity(self.fields.len() + 1);
                    let mut str_field_count = 0;
                    let mut int_field_count = 0;
                    for field in &self.fields {
                        match database.column_schema_map.get(field).unwrap().data_type {
                            DataType::String => {
                                let value = key.strs.get(str_field_count).unwrap();
                                map.insert(
                                    field.clone(),
                                    match value {
                                        None => Value::Null,
                                        Some(value) => Value::String(value.clone()),
                                    },
                                );
                                str_field_count += 1;
                            }
                            DataType::Integer => {
                                let value = key.ints.get(int_field_count).unwrap();
                                map.insert(
                                    field.clone(),
                                    match value {
                                        None => Value::Null,
                                        Some(value) => Value::Number(serde_json::Number::from(*value)),
                                    },
                                );
                                int_field_count += 1;
                            }
                            DataType::Date => {
                                let value = key.ints.get(int_field_count).unwrap();
                                map.insert(
                                    field.clone(),
                                    match value {
                                        None => Value::Null,
                                        Some(value) => {
                                            Value::String(decode_date_from_int(value).format("%Y-%m-%d").to_string())
                                        }
                                    },
                                );
                                int_field_count += 1;
                            }
                        }
                    }
                    map.insert(
                        "count".to_string(),
                        Value::Number(serde_json::Number::from(*count as i32)),
                    );
                    Value::Object(map)
                })
                .collect(),
        );
        serde_json::to_string(&json_value).unwrap()
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct AggregationKey {
    pub strs: Vec<Option<String>>,
    pub ints: Vec<Option<i32>>,
}

impl AggregationKey {
    pub fn new() -> Self {
        AggregationKey {
            strs: Vec::new(),
            ints: Vec::new(),
        }
    }
    pub fn put_str(&mut self, s: Option<String>) {
        self.strs.push(s);
    }
    pub fn put_int(&mut self, i: Option<i32>) {
        self.ints.push(i);
    }
}

pub struct DetailsQuery {
    pub fields: Vec<String>,
}

impl DetailsQuery {
    pub fn from_json_value(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            if let Value::Array(fields) = obj.get("fields")? {
                let mut fields_parsed = Vec::new();
                for f in fields {
                    if let Value::String(f) = f {
                        fields_parsed.push(f.to_string());
                    } else {
                        return None;
                    }
                }
                return Some(DetailsQuery { fields: fields_parsed });
            }
        }
        None
    }
}

pub struct NucSequencesQuery {
    pub aligned: bool,
}

impl NucSequencesQuery {
    pub fn from_json_value(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            if let Value::Bool(aligned) = obj.get("aligned")? {
                return Some(NucSequencesQuery {
                    aligned: aligned.clone(),
                });
            }
        }
        None
    }
}

pub struct NucMutationsQuery {
    pub min_proportion: f64,
    pub include_deletions: bool,
}

impl NucMutationsQuery {
    pub fn from_json_value(json: &Value) -> Option<Self> {
        if let Value::Object(obj) = json {
            if let Value::Number(min_proportion) = obj.get("minProportion")? {
                if let Value::Bool(include_deletions) = obj.get("includeDeletions")? {
                    return Some(NucMutationsQuery {
                        min_proportion: min_proportion.as_f64()?,
                        include_deletions: *include_deletions,
                    });
                }
            }
        }
        None
    }

    pub fn evaluate(&self, filtered: &Vec<bool>, database: &Database) -> NucMutationsQueryResult {
        let mut ids = Vec::new();
        for (i, b) in filtered.iter().enumerate() {
            if *b {
                ids.push(i as u32);
            }
        }
        let mut counts: Vec<MutationCount> = database
            .nuc_mutation_store
            .count_mutations(&ids)
            .into_iter()
            .filter(|count| count.proportion >= self.min_proportion)
            .collect();
        if !self.include_deletions {
            counts = counts
                .into_iter()
                .filter(|count| count.mutation.to != NucCode::GAP)
                .collect();
        }
        NucMutationsQueryResult { counts }
    }
}

pub struct NucMutationsQueryResult {
    counts: Vec<MutationCount>,
}

impl NucMutationsQueryResult {
    pub fn to_json(&self) -> String {
        let json_value = Value::Array(
            self.counts
                .iter()
                .map(|count| {
                    let mut map = serde_json::Map::new();
                    map.insert("mutation".to_string(), Value::String(count.mutation.to_string()));
                    map.insert(
                        "count".to_string(),
                        Value::Number(serde_json::Number::from(count.count)),
                    );
                    map.insert(
                        "proportion".to_string(),
                        Value::Number(serde_json::Number::from_f64(count.proportion).unwrap()),
                    );
                    Value::Object(map)
                })
                .collect(),
        );
        serde_json::to_string(&json_value).unwrap()
    }
}
