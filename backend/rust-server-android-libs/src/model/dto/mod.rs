use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct Storage {
    pub name: String,
    pub id: String,
    pub type_: i32,
    pub base_path: String
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


#[derive(Serialize, Deserialize, Debug)]
pub struct FileItem {
    pub name: String,
    pub path: String,
    pub is_dir: bool,
}

// FileItem to Json
impl FileItem {
    pub fn to_json(&self) -> String {
        serde_json::to_string(&self).unwrap()
    }
}