pub enum CommonOptionCode {
    GetBasePathListRequest = 1,
    GetBasePathListResponse = 2,
    GetChildrenPathListRequest = 3,
    GetChildrenPathListResponse = 4,
    MessageToSession = 5, // client or server 发送会话中的消息
    ClientRequestSessionFile = 6, // client 请求下载会话中的文件
    ClientFileToSessionPieceAcknowledge = 7, // server 收到一片上传的文件后回复 client 已收到，可以发送下一片
}

impl From<i32> for CommonOptionCode {
    fn from(code: i32) -> Self {
        match code {
            1 => CommonOptionCode::GetBasePathListRequest,
            2 => CommonOptionCode::GetBasePathListResponse,
            3 => CommonOptionCode::GetChildrenPathListRequest,
            4 => CommonOptionCode::GetChildrenPathListResponse,
            5 => CommonOptionCode::MessageToSession,
            6 => CommonOptionCode::ClientRequestSessionFile,
            7 => CommonOptionCode::ClientFileToSessionPieceAcknowledge,
            _ => panic!("eerr"),
        }
    }
}

