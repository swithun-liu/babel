use actix::{Actor, Context, Handler, Message, Recipient};
use actix_web::web::Bytes;
use log::{debug, info};
use rand::rngs::ThreadRng;
use crate::message_front_end_to_kernel;
use crate::model::communicate_models::MessageBinaryDTO;

#[derive(Message)]
#[rtype(result = "()")]
pub struct ConnectSessionMessage(pub String);

#[derive(Message)]
#[rtype(result = "()")]
#[derive(Debug)]
pub struct FrontEndMessage {
    pub msg: String,
}

#[derive(Message)]
#[rtype(result = "()")]
#[derive(Debug)]
pub struct FrontEndMessageBinary {
    pub msg: Bytes,
}
#[derive(Message)]
#[rtype(result = "()")]
pub struct KernelToFrontEndMessage {
    pub msg: String,
}
#[derive(Message)]
#[rtype(result = "()")]
pub struct FronterConnectMessage {
    pub addr: Recipient<ConnectSessionMessage>
}


pub struct ConnectServer {
    fronter: Option<Recipient<ConnectSessionMessage>>,
    rng: ThreadRng
}

impl ConnectServer {

    pub fn new() -> ConnectServer {
        ConnectServer {
            fronter: None,
            rng: rand::thread_rng(),
        }
    }

    pub fn send_text(&self, message: &str) {
        debug!("swithun-xxxx [send_command] {}", message);
        match &self.fronter {
            None => { }
            Some(front) => {
                front.do_send(ConnectSessionMessage(message.to_owned()))
            }
        }
    }

}

impl Actor for ConnectServer {
    type Context = Context<Self>;
}

impl Handler<FronterConnectMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: FronterConnectMessage, ctx: &mut Self::Context) -> Self::Result {
        debug!("handle fronter Connect");
        self.send_text("fronter Connect");
        self.fronter = Some(msg.addr);
        self.send_text("fronter Connected");
    }
}

impl Handler<KernelToFrontEndMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: KernelToFrontEndMessage, ctx: &mut Self::Context) -> Self::Result {
        self.send_text(msg.msg.as_str())
    }
}


impl Handler<FrontEndMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: FrontEndMessage, ctx: &mut Self::Context) -> Self::Result {
        debug!("kernel receive: {:?}", msg);
        let front_end_msg_json_str = msg.msg.as_str();
        let front_end_msg_json_struct = serde_json::from_str::<crate::communicate_models::MessageTextDTO>(front_end_msg_json_str);

        match front_end_msg_json_struct {
            Ok(kernel_and_front_end_json) => {
                crate::handle_android_front_end_response(kernel_and_front_end_json)
            }
            Err(_) => {
                debug!("kernel receive front_end_mgs_json parse failed")
            }
        }
    }
}

impl Handler<FrontEndMessageBinary> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: FrontEndMessageBinary, ctx: &mut Self::Context) -> Self::Result {
        let dto = MessageBinaryDTO::from_bytes(msg.msg.to_vec().as_slice());

        match dto {
            None => {
                debug!("FrontEndMessageBinary parse failed")
            }
            Some(dto) => {
                debug!("FrontEndMessageBinary parse suc {}", dto.content_id);
                message_front_end_to_kernel(dto)
            }
        }

    }
}