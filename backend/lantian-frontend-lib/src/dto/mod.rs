use std::collections::HashMap;
use serde::{Deserialize, Serialize};

pub enum OptionCode {
    CONNECT_SERVER = 1,
    WS_TEXT_MSG = 3
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Storage {
    name: String,
    id: String,
    type_: i32,
    base_path: String
}
// Storage to json
impl Storage {
    pub fn to_json(&self) -> String {
        serde_json::to_string(&self).unwrap()
    }
}

pub enum StorageType {
    LOCAL_INNER = 0,
    X_EXPLORE = 1
}

// i32 to StorageType
impl From<i32> for StorageType {
    fn from(code: i32) -> Self {
        match code {
            0 => StorageType::LOCAL_INNER,
            1 => StorageType::X_EXPLORE,
            _ => StorageType::LOCAL_INNER
        }
    }
}
