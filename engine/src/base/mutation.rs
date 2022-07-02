use crate::base::constants::NucCode;
use std::fmt::{Display, Formatter};
use std::hash::Hash;
use std::str::FromStr;

#[derive(Clone, Hash, Eq, PartialEq)]
pub struct RawMutation<T>
where
    T: Clone + Hash + Eq + PartialEq + Display,
{
    pub position: usize,
    pub to: T,
}

impl<T> Display for RawMutation<T>
where
    T: Clone + Hash + Eq + PartialEq + Display,
{
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}{}", self.position, self.to)
    }
}

pub type NucMutation = RawMutation<NucCode>;

impl FromStr for NucMutation {
    type Err = ();

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let chars: Vec<char> = s.chars().collect();
        let to = NucCode::from_byte(&(*chars.last().unwrap() as u8)).ok_or(())?;
        if chars[0].is_digit(10) {
            // We expect the format 1234A.
            Ok(NucMutation {
                position: s[0..s.len() - 1].parse().unwrap(),
                to,
            })
        } else {
            // We expect the format C1234A.
            Ok(NucMutation {
                position: s[1..s.len() - 1].parse().unwrap(),
                to,
            })
        }
    }
}
