use serde::{Deserialize, Serialize};
use log::debug;

#[derive(Serialize, Deserialize, Debug)]
pub struct MessageTextDTO {
    pub(crate) uuid: String,
    pub(crate) code: i32,
    pub(crate) content: String,
    pub(crate) content_type: i32,
}

pub enum ContentType {
    TEXT = 0,
    IMAGE = 1,
}

pub struct MessageBinaryDTO {
    // 36 bytes
    pub(crate) content_id: String,
    // 4 bytes
    pub(crate) seq: i32,
    // max 60 * 1024 bytes
    pub(crate) payload: Vec<u8>,
}

impl MessageTextDTO {

    pub fn to_json_str(self) -> String {
        serde_json::to_string(&self).unwrap()
    }

}

impl MessageBinaryDTO {
    pub fn from_bytes(bytes: &[u8]) -> Option<Self> {
        if bytes.len() < 40 {
            return None;
        }
        let content_id = String::from_utf8_lossy(&bytes[..36]).trim_end_matches(char::from(0)).to_string();
        debug!("MessageDTO # from_bytes # content_id: {}", content_id);
        let seq = i32::from_be_bytes([bytes[36], bytes[37], bytes[38], bytes[39]]);
        debug!("MessageDTO # from_bytes # seq: {}", seq);
        let payload = bytes[40..].to_vec();
        Some(Self {
            content_id,
            seq,
            payload,
        })
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        let mut result = Vec::with_capacity(40 + self.payload.len());
        result.extend(self.content_id.as_bytes()); // uid
        result.extend(&self.seq.to_be_bytes()); // seq
        result.extend(&self.payload); // payload
        result.into()
    }

}

pub fn generateUUID() -> String {
    let uid = uuid::Uuid::new_v4();
    let uid_utf8 = format!("{:?}", uid);
    debug!("new uid: {} - {}", uid_utf8 , uid_utf8.as_bytes().len());
    return uid_utf8
}
