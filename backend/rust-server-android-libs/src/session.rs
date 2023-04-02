use std::collections::HashMap;
use std::fs::{File, self, OpenOptions};
use std::io::{Seek, SeekFrom, Write};
use std::string::ToString;
use std::time::{Instant, Duration};
use std::usize;

use actix::{Addr, Actor, StreamHandler, ActorContext, AsyncContext, Handler, WrapFuture, ActorFutureExt, fut, ContextFutureSpawner};
use actix_web_actors::ws;
use log::debug;

use crate::server;

const HEARTBEAT_INTERAL: Duration = Duration::from_secs(5);

const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

const PARENT_PATH: &str = "/storage/emulated/0/";

const TEMP_FILE_NAME: &str = "temp";

pub struct ClientSession {
    pub id: usize,

    pub hb: Instant,

    pub name: Option<String>,

    pub transfer_server: Addr<server::ClientServer>,

    pub uploading_file: HashMap<String, String>,
}

impl ClientSession {

    fn hb(&self, ctx: &mut ws::WebsocketContext<Self>) {
        ctx.run_interval(HEARTBEAT_INTERAL, |act, ctx| {
            if Instant::now().duration_since(act.hb) > CLIENT_TIMEOUT {
                println!("Websocket client heartbeat failed, disconnecting!");

                act.transfer_server.do_send(server::Disconnect { id: act.id, });

                ctx.stop();

                return;
            }

            ctx.ping(b"");
        });
    }
}

impl Actor for ClientSession {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        self.hb(ctx);

        let addr = ctx.address();
        println!("WsChatSession # Actor # started $ ");

        self.transfer_server.send(server::Connect {
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
        self.transfer_server.do_send(server::Disconnect { id : self.id });
        actix::Running::Stop
    }
}

impl Handler<server::SessionMessage> for ClientSession {
    type Result = ();

    fn handle(&mut self, msg: server::SessionMessage, ctx: &mut Self::Context) -> Self::Result {
        ctx.text(msg.0)
    }
}

impl StreamHandler<Result<ws::Message, ws::ProtocolError>> for ClientSession {
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
                let msg = text.trim();

                self.transfer_server.do_send(server::ClientMessage {
                    id: self.id,
                    msg: msg.to_owned(),
                })
            }
            ws::Message::Binary(byte) => {
                debug!("WsChatSession - StreamHandler - handle - Binary");

                let temp_file_path = format!("{}{}", PARENT_PATH, "swithun/temp");

                let dto = crate::session::MessageDTO::from_bytes(&byte);
                match dto {
                    Some(dto) => {
                        debug!("{} # parse dto suc: seq: {}", tag, dto.seq);
                        match dto.seq {
                            0 => {
                                // 第一片里面，payload是文件名
                                match String::from_utf8(dto.payload) {
                                    Ok(file_name) => {
                                        let new_file_path = format!("{}{}{}", PARENT_PATH, "swithun/", file_name);
                                        let new_file_path_clone = new_file_path.clone();
                                        self.uploading_file.insert(dto.content_id, new_file_path_clone);
                                        debug!("parse file name suc: new_path {}", new_file_path);
                                        match fs::rename(&temp_file_path, &new_file_path) {
                                            Ok(_) => {
                                                debug!("rename file suc");
                                            },
                                            Err(e) => {
                                                debug!("rename file err: {}", e);
                                            },
                                        }
                                    },
                                    Err(e) => {
                                        debug!("parse file name err: {}", e);
                                    },
                                }
                            }
                            // 最后一片
                            -1 => {
                                
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


                let mut file = match File::create(&temp_file_path) {
                    Ok(f) => {
                        debug!("create file suc");
                        Some(f)
                    },
                    Err(e) => {
                        debug!("create file err: {}", e);
                        None
                    }
                };
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

pub struct MessageDTO {
    content_id: String,
    // 36 bytes
    seq: i32,
    // 4 bytes
    payload: Vec<u8>,
}

impl MessageDTO {
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
}
