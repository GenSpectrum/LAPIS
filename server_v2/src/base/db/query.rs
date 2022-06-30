use crate::db::query::QuerySelect::{Aggregated, Details, NucSequences};
use crate::{filters, Filter};
use serde_json::Value;

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
