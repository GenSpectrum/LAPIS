use chrono::{Duration, NaiveDate};

pub fn parse_date(s: &str) -> Option<NaiveDate> {
    NaiveDate::parse_from_str(s, "%Y-%m-%d").ok()
}

pub fn encode_date_as_int(date: &NaiveDate) -> i32 {
    let x = *date - NaiveDate::from_ymd(1970, 1, 1);
    x.num_days() as i32
}

pub fn decode_date_from_int(i: &i32) -> NaiveDate {
    NaiveDate::from_ymd(1970, 1, 1) + Duration::days(*i as i64)
}
