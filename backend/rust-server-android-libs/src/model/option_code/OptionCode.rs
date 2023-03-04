pub enum CommonOptionCode {
    GET_BASE_PATH_LIST_REQUEST = 1,
    GET_BASE_PATH_LIST_RESPONSE = 2,
    GET_CHILDREN_PATH_LIST_REQUEST = 3,
    GET_CHILDREN_PATH_LIST_RESPONSE = 4
}

impl From<i32> for CommonOptionCode {
    fn from(code: i32) -> Self {
        match code {
            1 => CommonOptionCode::GET_BASE_PATH_LIST_REQUEST,
            2 => CommonOptionCode::GET_BASE_PATH_LIST_RESPONSE,
            3 => CommonOptionCode::GET_CHILDREN_PATH_LIST_REQUEST,
            4 => CommonOptionCode::GET_CHILDREN_PATH_LIST_RESPONSE,
            _ => panic!("eerr"),
        }
    }
}
