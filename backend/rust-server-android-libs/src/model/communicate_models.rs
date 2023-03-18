use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Debug)]
pub struct CommonCommunicateJsonStruct {
    pub(crate) uuid: String,
    pub(crate) code: i32,
    pub(crate) content: String,
    pub(crate) content_type: i32,
}
