use std::any::Any;
use std::collections::HashMap;
use std::fmt::Binary;
use std::fs::{File, OpenOptions};
use std::io::{Seek, SeekFrom, Write};
use std::path::Path;
use std::string::ToString;
use std::time::{Duration, Instant};
use std::usize;

use actix::{Actor, ActorContext, ActorFutureExt, Addr, AsyncContext, ContextFutureSpawner, fut, Handler, StreamHandler, WrapFuture};
use actix_web_actors::ws;
use log::debug;
use uuid::Uuid;

use crate::session;

const HEARTBEAT_INTERAL: Duration = Duration::from_secs(5);

const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

const PARENT_PATH: &str = "/storage/emulated/0/";

const TEMP_FILE_NAME: &str = "temp";

pub struct Session {
    pub id: usize,

    pub hb: Instant,

    pub name: Option<String>,

    pub session_server: Addr<session::session_server::SessionServer>,

    pub uploading_file: HashMap<String, String>,
}

impl Session {

    fn hb(&self, ctx: &mut ws::WebsocketContext<Self>) {
        ctx.run_interval(HEARTBEAT_INTERAL, |act, ctx| {
            if Instant::now().duration_since(act.hb) > CLIENT_TIMEOUT {
                println!("Websocket client heartbeat failed, disconnecting!");

                act.session_server.do_send(session::session_server::Disconnect { id: act.id, });

                ctx.stop();

                return;
            }

            ctx.ping(b"");
        });
    }
}

impl Actor for Session {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        self.hb(ctx);

        let addr = ctx.address();
        println!("WsChatSession # Actor # started $ ");

        self.session_server.send(session::session_server::Connect {
            addr: addr.recipient(),
        })
        .into_actor(self)
        .then(|res, act, ctx| {
            match res {
                Ok(res) => {
                    println!("{}", ("WsChatSession # Actor # started # Connect # res".to_string() + &res.to_string()).as_str());
                    act.id = res
                },
                Err(_) => {
                    ctx.stop();
                },
            }
            fut::ready(())
        }).wait(ctx);

    }

    fn stopping(&mut self, ctx: &mut Self::Context) -> actix::Running {
        self.session_server.do_send(session::session_server::Disconnect { id : self.id });
        actix::Running::Stop
    }
}

impl Handler<session::session_server::SessionHolder> for Session {
    type Result = ();

    fn handle(&mut self, msg: session::session_server::SessionHolder, ctx: &mut Self::Context) -> Self::Result {
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
                let json_msg = text.trim();

                // 将客户端消息发送给session_server处理
                self.session_server.do_send(session::session_server::SessionMessage {
                    session_id: self.id,
                    json_msg: json_msg.to_owned(),
                })
            }
            ws::Message::Binary(byte) => {
                debug!("WsChatSession - StreamHandler - handle - Binary");

                let temp_file_path = format!("{}{}", PARENT_PATH, "swithun/temp");

                let dto = crate::model::communicate_models::MessageBinaryDTO::from_bytes(&byte);
                match dto {
                    Some(dto) => {
                        debug!("{} # parse dto suc: seq: {}", tag, dto.seq);
                        match dto.seq {
                            0 => {
                                // 第一片里面，payload是文件名
                                match String::from_utf8(dto.payload) {
                                    Ok(file_name) => {
                                        let mut new_file_path_str = format!("{}{}{}", PARENT_PATH, "swithun/", file_name);
                                        let mut new_file_path = Path::new(new_file_path_str.as_str());

                                        let mut num = 0;
                                        while new_file_path.exists() {
                                            num += 1;
                                            new_file_path_str.push_str(&(format!("({})", num).as_str()));
                                            new_file_path = Path::new(new_file_path_str.as_str());
                                        }

                                        let mut new_file_path_str_clone = new_file_path_str.clone();
                                        self.uploading_file.insert(dto.content_id, new_file_path_str_clone);
                                        debug!("parse file name suc: new_path {}", new_file_path_str);

                                        match File::create(new_file_path) {
                                            Ok(new_file) => {
                                                debug!("create new file suc");
                                            }
                                            Err(e) => {
                                                debug!("create new file failed: {}", e);
                                            }
                                        }
                                    },
                                    Err(e) => {
                                        debug!("parse file name err: {}", e);
                                    },
                                }
                            }
                            // 最后一片，通知结束，payload为空
                            -1 => {
                                match self.uploading_file.get(&dto.content_id) {
                                    Some(file_path) => {
                                        debug!("find uploading file suc: {} - {}", dto.content_id, file_path);

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
                                        match serde_json::to_string(&model)  {
                                            Ok(model_json) => {
                                                self.session_server.do_send(session::session_server::SessionMessage {
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
                            // 其他分片payload都是数据
                            seq => {
                                debug!("{} # file seq {}", tag, dto.seq);
                                let chunk_size = 60 * 1024;
                                let offset = (seq as u64 - 1) * chunk_size;

                                match self.uploading_file.get(&dto.content_id) {
                                    Some(file_path) => {
                                        match OpenOptions::new().read(true).write(true).create(true).open(file_path) {
                                            Ok(mut file) => {
                                                match file.seek(SeekFrom::Start(offset)) {
                                                    Ok(mut seeked_file) => {
                                                        file.write_all(dto.payload.as_slice());
                                                    },
                                                    Err(e) => {
                                                        debug!("{} file seek to {} failed: {}", tag, offset, e);
                                                    },
                                                }
                                            }
                                            Err(e) => {
                                                debug!("{} open file failed: {}", tag, e);
                                            },
                                        }
                                    },
                                    None => {
                                        debug!("{} get file from map failed", tag);
                                    },
                                }

                            }
                        }
                    }
                    None => {
                        debug!("{} # parse dto failed", tag);
                    }
                }
            }
            ws::Message::Continuation(_) => {
                debug!("WsChatSession - StreamHandler - handle - Continuation");
                ctx.stop();
            }
            ws::Message::Close(reason) => {
                debug!("WsChatSession - StreamHandler - handle - Close");
                ctx.close(reason);
                ctx.stop();
            }
            ws::Message::Nop => {
                debug!("WsChatSession - StreamHandler - handle - Nop");
            }
        }
    }
}
