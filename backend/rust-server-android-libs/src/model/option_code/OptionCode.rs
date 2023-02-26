pub enum CommonOptionCode {
    GET_BASE_PATH_LIST_REQUEST = 1,
    GET_BASE_PATH_LIST_RESPONSE = 2,

}

impl CommonOptionCode {
    pub(crate) fn from_value(code: i32) -> Option<CommonOptionCode> {
        match code {
            1 => Some(CommonOptionCode::GET_BASE_PATH_LIST_REQUEST),
            2 => Some(CommonOptionCode::GET_BASE_PATH_LIST_RESPONSE),
            _ => None,
        }
    }
}

