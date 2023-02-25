use actix::{Actor, Context, Handler, Message, Recipient};
use log::debug;
use rand::rngs::ThreadRng;

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


impl Handler<FronterMessage> for ConnectServer {
    type Result = ();

    fn handle(&mut self, msg: FronterMessage, ctx: &mut Self::Context) -> Self::Result {
        debug!("swithun-xxxx # rust # Handler<FronterMessage>");
        self.send_command(msg.msg.as_str())
    }
}
