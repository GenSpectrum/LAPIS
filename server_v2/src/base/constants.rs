use crate::base::constants::NucCode::*;

#[derive(PartialEq)]
pub enum NucCode {
    A,
    C,
    G,
    T,
    U,
    M,
    R,
    W,
    S,
    Y,
    K,
    V,
    H,
    D,
    B,
    N,
    X,
    GAP,
}

impl NucCode {
    pub fn from_byte(byte: u8) -> Option<NucCode> {
        match byte {
            0x41 => Some(A),
            0x43 => Some(C),
            0x47 => Some(G),
            0x54 => Some(T),
            0x55 => Some(U),
            0x4D => Some(M),
            0x52 => Some(R),
            0x57 => Some(W),
            0x53 => Some(S),
            0x59 => Some(Y),
            0x4B => Some(K),
            0x56 => Some(V),
            0x48 => Some(H),
            0x44 => Some(D),
            0x42 => Some(B),
            0x4E => Some(N),
            0x58 => Some(X),
            0x2D => Some(GAP),
            _ => None,
        }
    }

    /// This function maps unexpected bytes to `N`
    pub fn from_byte_ignore_weird(byte: u8) -> NucCode {
        match byte {
            0x41 => A,
            0x43 => C,
            0x47 => G,
            0x54 => T,
            0x55 => U,
            0x4D => M,
            0x52 => R,
            0x57 => W,
            0x53 => S,
            0x59 => Y,
            0x4B => K,
            0x56 => V,
            0x48 => H,
            0x44 => D,
            0x42 => B,
            0x4E => N,
            0x58 => X,
            0x2D => GAP,
            _ => N,
        }
    }
}
