use crate::base::constants::NucCode;
use chrono::NaiveDate;

pub trait Operator {}

pub struct And {
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for And {}

pub struct Or {
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for Or {}

pub struct Neg {
    pub child: dyn Operator,
}

impl Operator for Neg {}

pub struct StrEq {
    pub column: String,
    pub value: String,
}

impl Operator for StrEq {}

pub struct DateBetw {
    pub column: String,
    pub from: Option<NaiveDate>,
    pub to: Option<NaiveDate>,
}

impl Operator for DateBetw {}

pub struct NucEq {
    pub position: u32,
    pub value: NucCode,
}

impl Operator for NucEq {}

pub struct NOf {
    pub n: u32,
    pub exactly: bool,
    pub children: Vec<Box<dyn Operator>>,
}

impl Operator for NOf {}

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
