use actix::{Actor, Addr, Context, Handler, Message, Recipient};
use log::debug;
use rand::rngs::ThreadRng;

use crate::model::option_code;

#[derive(Message)]
#[rtype(result = "()")]
pub struct ConnectSessionMessage(pub String);
#[derive(Message)]
#[rtype(result = "()")]
pub struct FronterMessage {
    pub msg: String,
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

    pub fn send_command(&self, message: &str) {
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
        self.send_command("fronter Connect");
        self.fronter = Some(msg.addr);
        self.send_command("fronter Connected");
    }
}

impl Handler<KernelToFrontEndMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: KernelToFrontEndMessage, ctx: &mut Self::Context) -> Self::Result {
        self.send_command(msg.msg.as_str())
    }
}


impl Handler<FronterMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: FronterMessage, ctx: &mut Self::Context) -> Self::Result {
        debug!("swithun-xxxx # rust # Handler<FronterMessage>");
        let data_str = msg.msg.as_str();
        let data_str_clone = data_str.clone();

        let json_struct_result = serde_json::from_str::<crate::communicate_models::CommonCommunicateJsonStruct>(data_str);

        match json_struct_result {
            Ok(kernel_and_front_end_json) => {
                let kernel_and_front_end_json_code_enum: Option<option_code::OptionCode::CommonOptionCode> = option_code::OptionCode::CommonOptionCode::from_value(kernel_and_front_end_json.code);
                match kernel_and_front_end_json_code_enum {
                    Some(coc) => {
                        match coc {
                            option_code::OptionCode::CommonOptionCode::GET_BASE_PATH_LIST_RESPONSE => {
                                debug!("GET_BASE_PATH_LIST_RESPONSE ");
                                crate::handle_android_front_end_response(kernel_and_front_end_json)
                            }
                            _ => {
                                debug!("other code")
                            }
                        }
                    }
                    None => {

                    }
                }
            }
            Err(_) => {

            }
        }
    }
}
