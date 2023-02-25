use serde::{Serialize, Deserialize};
use serde_json;

#[derive(Serialize, Deserialize, Debug)]
pub struct CommunicateJson {
    pub(crate) uuid: String,
    pub(crate) code: i32,
    pub(crate) content: String,
}
