use std::any::Any;
use std::collections::HashMap;
use std::fmt::{Binary, format};
use std::fs::{File, OpenOptions};
use std::io::{Seek, SeekFrom, Write};
use std::path::{Path, PathBuf};
use std::string::ToString;
use std::time::{Duration, Instant};
use std::usize;

use actix::{Actor, ActorContext, ActorFutureExt, Addr, AsyncContext, ContextFutureSpawner, fut, Handler, StreamHandler, WrapFuture};
use actix_web::web::Bytes;
use actix_web_actors::ws;
use actix_web_actors::ws::WebsocketContext;
use log::debug;
use uuid::Uuid;
use crate::model::communicate_models::{MessageBinaryDTO, MessageTextDTO};

use crate::session;

const HEARTBEAT_INTERAL: Duration = Duration::from_secs(5);

const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

const PARENT_PATH: &str = "/storage/emulated/0/";

const TEMP_FILE_NAME: &str = "temp";

pub struct Session {
    pub id: usize,

    pub hb: Instant,

    pub name: Option<String>,

    pub session_server_ref: Addr<session::session_server::SessionServer>,

    pub uploading_file: HashMap<String, String>,
}

impl Session {
    fn hb(&self, ctx: &mut ws::WebsocketContext<Self>) {
        ctx.run_interval(HEARTBEAT_INTERAL, |act, ctx| {
            if Instant::now().duration_since(act.hb) > CLIENT_TIMEOUT {
                println!("Websocket client heartbeat failed, disconnecting!");

                act.session_server_ref.do_send(session::session_server::Disconnect { id: act.id });

                ctx.stop();

                return;
            }

            ctx.ping(b"");
        });
    }

    fn handle_receive_text(&self, text: &str) {
        // 将客户端消息发送给session_server处理
        self.session_server_ref.do_send(session::session_server::SessionToSessionServerMessage {
            session_id: self.id,
            json_msg: text.to_owned(),
        })
    }

    /// 其他分片payload都是数据
    fn handle_receive_sequence_slice(&mut self, dto: MessageBinaryDTO) {

        let tag = "WsChatSession#StreamHandler#handle sequence";

        debug!("{} # file seq {}", tag, dto.seq);
        // 每片60k
        let chunk_size = 60 * 1024;
        // 计算偏移量
        let offset = (dto.seq as u64 - 1) * chunk_size;

        // 根据content_id找到文件路径
        match self.uploading_file.get(&dto.content_id) {
            Some(file_path) => {
                match OpenOptions::new().read(true).write(true).create(true).open(file_path) {
                    Ok(mut file) => {
                        // 定位到偏移量
                        match file.seek(SeekFrom::Start(offset)) {
                            Ok(mut seeked_file) => {
                                // 写入数据
                                match file.write_all(dto.payload.as_slice()) {
                                    Ok(_) => {}
                                    Err(e) => debug!("{} file write to {} failed: {}", tag, offset, e)
                                }
                            }
                            Err(e) => debug!("{} file seek to {} failed: {}", tag, offset, e)
                        }
                    }
                    Err(e) => debug!("{} open file failed: {}", tag, e)
                }
            }
            None => debug!("{} get file from map failed", tag)
        }
    }

    /// 最后一片，通知结束，payload为空
    fn handle_receive_binary_file_final_slice(&mut self, dto: MessageBinaryDTO) {
        let tag = "WsChatSession#StreamHandler#handle final";

        debug!("{} # file seq {} # final", tag, dto.seq);

        match self.uploading_file.get(&dto.content_id) {
            Some(file_path) => {
                debug!("find uploading file suc: {} - {}", dto.content_id, file_path);

                // 根据文件路径，获取文件名
                let file_name = Path::new(file_path)
                    .file_name()
                    .and_then(|os_str| os_str.to_str())
                    .unwrap_or("");
                debug!("file name: {}", file_name);

                let new_uuid: String = format!("{}", Uuid::new_v4());
                let model = crate::model::communicate_models::MessageTextDTO {
                    uuid: new_uuid,
                    code: crate::model::option_code::OptionCode::CommonOptionCode::TRANSFER_DATA as i32,
                    content: file_name.to_string(),
                    content_type: crate::model::communicate_models::ContentType::IMAGE as i32,
                };
                match serde_json::to_string(&model) {
                    Ok(model_json) => {
                        // 通知session_server，文件上传完成
                        self.session_server_ref.do_send(session::session_server::SessionToSessionServerMessage {
                            session_id: self.id,
                            json_msg: model_json,
                        });
                    }
                    Err(_) => {
                        debug!("parse model json failed")
                    }
                }

            }
            None => {
                debug!("find uploading file failed: {}", dto.content_id)
            }
        }
    }

    /// 第一片里面，payload是文件绝对路径
    fn handle_receive_binary_file_0_slice(&mut self, dto: MessageBinaryDTO) {
        let tag = "WsChatSession#StreamHandler#handle_receive_binary_file_0_slice";

        debug!("{} # file seq {} # first", tag, dto.seq);

        match String::from_utf8(dto.payload) {
            Ok(file_path) => {
                let mut ood_new_file_path = PathBuf::from(file_path.as_str());
                let mut uod_new_file_path = ood_new_file_path.clone();

                // 父路径
                let mut file_parent_path = ood_new_file_path.parent().unwrap().to_str().unwrap_or("");
                // 文件名(不包括后缀)
                let mut file_stem = ood_new_file_path.file_stem().unwrap().to_str().unwrap_or("");
                // 文件路径(不包括后缀)
                let mut file_parent_path_with_file_stem = format!("{}/{}", file_parent_path, file_stem);
                // 文件后缀
                let mut ood_file_suffix = ood_new_file_path.extension().unwrap_or_default().to_str().unwrap_or("");
                let mut uod_file_suffix = ood_file_suffix.clone().to_string();
                if ood_file_suffix.len() > 0 {
                    uod_file_suffix = format!(".{}", &ood_file_suffix).to_owned();
                }
                /// 使用 new_file_path 生成File对象，如果文件已存在，则在文件名后面加上(1)，(2)，(3)...，但是注意后缀名不能变
                // 文件标号
                let mut num = 1;
                while uod_new_file_path.exists() {
                    // 增加文件标号
                    uod_new_file_path = PathBuf::from(
                        format!(
                            "{}({}){}",
                            file_parent_path_with_file_stem ,
                            num,
                            uod_file_suffix
                        ));
                    num += 1
                };

                debug!("{} # parse file name suc: new_path {}", tag, uod_new_file_path.to_str().unwrap_or(""));

                // 创建父文件夹
                let parent_path = uod_new_file_path.parent().unwrap().to_str().unwrap_or("");
                debug!("{} # parent path: {}", tag, parent_path);

                std::fs::create_dir_all(parent_path).unwrap_or_else(|why| {
                    debug!("{} # create dir all failed: {}", tag, why);
                });

                // 创建文件
                match File::create(&uod_new_file_path) {
                    Ok(file) => {
                        debug!("{} # create file suc: {}", tag, uod_new_file_path.to_str().unwrap_or(""));
                        // 将文件路径存入map
                        self.uploading_file.insert(dto.content_id.clone(), uod_new_file_path.to_str().unwrap_or("").to_string());
                    }
                    Err(why) => {
                        debug!("{} # create file failed: {}", tag, why);
                    }
                }
            }
            Err(e) => {
                debug!("parse file name err: {}", e);
            }
        }
    }

    fn handle_receive_binary(&mut self, byte: Bytes, ctx: &mut WebsocketContext<Session>) {

        let tag = "WsChatSession - StreamHandler";

        let dto = MessageBinaryDTO::from_bytes(&byte);
        match dto {
            Some(dto) => {
                debug!("{} # parse dto suc haha: seq: {}", tag, dto.seq);
                let remember_dto_content_id = dto.content_id.clone();

                // 处理收到的数据
                match dto.seq {
                    0 => self.handle_receive_binary_file_0_slice(dto),
                    -1 => self.handle_receive_binary_file_final_slice(dto),
                    seq => self.handle_receive_sequence_slice(dto),
                }

                // 回复收到
                let receive_response_uuid: String = format!("{}", Uuid::new_v4());
                let receive_response = MessageTextDTO {
                    uuid: receive_response_uuid,
                    code: crate::model::option_code::OptionCode::CommonOptionCode::SEND_FILE_PIECE_RESPONSE as i32,
                    content: remember_dto_content_id,
                    content_type: crate::model::communicate_models::ContentType::TEXT as i32,
                };
                ctx.text(receive_response.to_json_str())
            }
            None => {
                debug!("{} # parse dto failed", tag);
            }
        }
    }

}

impl Actor for Session {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        self.hb(ctx);

        let addr = ctx.address();
        println!("WsChatSession # Actor # started $ ");

        self.session_server_ref.send(session::session_server::Connect {
            addr: addr.recipient(),
        })
            .into_actor(self)
            .then(|res, act, ctx| {
                match res {
                    Ok(res) => {
                        println!("{}", ("WsChatSession # Actor # started # Connect # res".to_string() + &res.to_string()).as_str());
                        act.id = res
                    }
                    Err(_) => {
                        ctx.stop();
                    }
                }
                fut::ready(())
            }).wait(ctx);
    }

    fn stopping(&mut self, ctx: &mut Self::Context) -> actix::Running {
        self.session_server_ref.do_send(session::session_server::Disconnect { id: self.id });
        actix::Running::Stop
    }
}

impl Handler<session::session_server::ServerMessage> for Session {
    type Result = ();

    fn handle(&mut self, msg: session::session_server::ServerMessage, ctx: &mut Self::Context) -> Self::Result {
        match msg.text {
            None => {
                match msg.binary {
                    None => {
                        debug!("Session # Handle # Session Holder: send nothing")
                    }
                    Some(binary) => {
                        debug!("Session # Handle # Session Holder: send binary");
                        ctx.binary(binary)
                    }
                }
            }
            Some(text) => {
                debug!("Session # Handle # Session Holder: send text: {}", text);
                ctx.text(text)
            }
        }
    }
}

impl StreamHandler<Result<ws::Message, ws::ProtocolError>> for Session {

    fn handle(&mut self, msg: Result<ws::Message, ws::ProtocolError>, ctx: &mut Self::Context) {
        let msg = match msg {
            Ok(msg) => msg,
            Err(_) => {
                ctx.stop();
                return;
            }
        };

        let tag = "WsChatSession - StreamHandler";

        match msg {
            ws::Message::Ping(msg) => {
                ctx.pong(&msg);
            }
            ws::Message::Pong(_) => {
                self.hb = Instant::now();
            }
            ws::Message::Text(text) => {
                debug!("WsChatSession - StreamHandler - handle - Text");
                self.handle_receive_text(text.trim());
            }
            ws::Message::Binary(byte) => {
                debug!("WsChatSession - StreamHandler - handle - Binary");
                self.handle_receive_binary(byte, ctx);
            }
            ws::Message::Continuation(_) => {
                debug!("WsChatSession - StreamHandler - handle - Continuation");
                ctx.stop();
            }
            ws::Message::Close(reason) => {
                debug!("WsChatSession - StreamHandler - handle - Close {:?}", reason);

                ctx.close(reason);
                ctx.stop();
            }
            ws::Message::Nop => {
                debug!("WsChatSession - StreamHandler - handle - Nop");
            }
        }
    }
}
