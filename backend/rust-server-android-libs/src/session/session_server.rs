use std::{
    collections::HashMap,
    sync::{
        atomic::{AtomicUsize, Ordering},
        Arc,
    },
};

use crate::model::option_code;
use actix::{Actor, Addr, Context, Handler, Message, Recipient};
use log::debug;
use rand::{rngs::ThreadRng, Rng};


use crate::model::communicate_models;


pub struct SessionServer {
    // <人的id，对应的recipient>
    sessions: HashMap<usize, Recipient<SessionHolder>>,
    // 访客人数
    visitor_count: Arc<AtomicUsize>,
    connect_server: Addr<crate::connect::connect_server::ConnectServer>,
    rng: ThreadRng,
}

impl SessionServer {
    pub fn new(
        visitor_count: Arc<AtomicUsize>,
        connect_server: Addr<crate::connect::connect_server::ConnectServer>,
    ) -> SessionServer {
        SessionServer {
            sessions: HashMap::new(),
            visitor_count,
            connect_server,
            rng: rand::thread_rng(),
        }
    }

    pub fn send_message(&self, message: &str) {
        println!("[ChatServer] [send_message] {}", message);

        for (id, rcp) in &self.sessions {
            rcp.do_send(SessionHolder(message.to_owned()))
        }
    }

    pub fn send_message_to_specific_recipient(&self, recipient_id: usize, message: &str) {
        for (id, rcp) in &self.sessions {
            if *id == recipient_id {
                rcp.do_send(SessionHolder(message.to_owned()))
            }
        }
    }
}

impl Actor for SessionServer {
    // simple Context for communicate with other actors
    type Context = Context<Self>;
}

impl Handler<Connect> for SessionServer {
    type Result = usize;

    fn handle(&mut self, msg: Connect, ctx: &mut Self::Context) -> Self::Result {
        println!("ChatServer # handle # Connect $ Someone joined");

        self.send_message("Someone joined");
        let id = self.rng.gen::<usize>();
        self.sessions.insert(id, msg.addr);

        let count = self.visitor_count.fetch_add(1, Ordering::SeqCst);

        self.send_message(&format!("Total visitors {count}"));

        id
    }
}

impl Handler<SessionMessage> for SessionServer {
    type Result = ();

    // 处理client发来的数据数据
    fn handle(&mut self, msg: SessionMessage, ctx: &mut Self::Context) -> Self::Result {
        let msg = msg.json_msg.as_str();
        let msg_clone = msg.clone();
        debug!("ChatServer # handle #ClientMessage {}", msg_clone);

        let json_struct_result =
            serde_json::from_str::<communicate_models::CommonCommunicateJsonStruct>(msg);

        match json_struct_result {
            Ok(communicate_json) => {
                match option_code::OptionCode::CommonOptionCode::try_from(communicate_json.code)
                    .unwrap()
                {
                    // client获取server的根目录下文件列表
                    option_code::OptionCode::CommonOptionCode::GET_BASE_PATH_LIST_REQUEST => {
                        crate::kernel_send_message_to_front_end(communicate_json)
                    }
                    // client给其他client发送消息/文件
                    option_code::OptionCode::CommonOptionCode::TRANSFER_DATA => {
                        self.send_message((msg_clone.to_owned()).as_str())
                    }
                    // 其他未知命令，直接转发给所有client
                    _ => self.send_message((msg_clone.to_owned() + "1!!!").as_str()),
                }
            }
            // client未按照定义格式发送数据包，直接将原数据转发给所有客client
            Err(_) => self.send_message((msg_clone.to_owned() + "2!!!").as_str()),
        }
    }
}

impl Handler<Disconnect> for SessionServer {
    type Result = ();

    fn handle(&mut self, msg: Disconnect, ctx: &mut Self::Context) -> Self::Result {
        println!("Someone disconnected");

        let mut old_size: usize = 0;
        if self.sessions.remove(&msg.id).is_some() {
            old_size = self
                .visitor_count
                .fetch_update(Ordering::SeqCst, Ordering::SeqCst, |x| Some(x - 1))
                .unwrap();
            println!("ChatServer # handle # Disconnect $ remove {}", msg.id);
        }
        self.send_message(
            ("Disconnect $ remove ".to_string()
                + &msg.id.to_string()
                + &" ".to_string()
                + &(old_size - 1).to_string()
                + &"left".to_string())
                .as_str(),
        )
    }
}

#[derive(Message)]
#[rtype(result = "()")]
pub struct SessionHolder(pub String);

#[derive(Message)]
#[rtype(result = "()")]
pub struct SessionMessage {
    pub session_id: usize,
    pub json_msg: String,
}

#[derive(Message)]
#[rtype(result = "()")]
pub struct Disconnect {
    pub id: usize,
}

#[derive(Message)]
#[rtype(usize)]
pub struct Connect {
    pub addr: Recipient<SessionHolder>,
}

