use std::time::{Instant, Duration};

use actix::{Addr, Actor, StreamHandler, ActorContext, AsyncContext, Handler, WrapFuture, ActorFutureExt, fut, ContextFutureSpawner};
use actix_web_actors::ws;
use log::debug;

use crate::server;

const HEARTBEAT_INTERAL: Duration = Duration::from_secs(5);

const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

pub struct ClientSession {
    pub id: usize,

    pub hb: Instant,

    pub name: Option<String>,

    pub transfer_server: Addr<server::ClientServer>
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
            },
        };

        match msg {
            ws::Message::Ping(msg) => {
                debug!("WsChatSession - StreamHandler - handle - Ping");
                ctx.pong(&msg);
            },
            ws::Message::Pong(_) => {
                debug!("WsChatSession - StreamHandler - handle - Pong");
                self.hb = Instant::now();
            },
            ws::Message::Text(text) => {
                debug!("WsChatSession - StreamHandler - handle - Text");
                let msg = text.trim();

                self.transfer_server.do_send(server::ClientMessage {
                    id: self.id,
                    msg: msg.to_owned(),
                })
            },
            ws::Message::Binary(byte) => {
                debug!("WsChatSession - StreamHandler - handle - Binary");
            },
            ws::Message::Continuation(_) => {
                debug!("WsChatSession - StreamHandler - handle - Continuation");
                ctx.stop();
            },
            ws::Message::Close(reason) => {
                debug!("WsChatSession - StreamHandler - handle - Close");
                ctx.close(reason);
                ctx.stop();
            },
            ws::Message::Nop => {
                debug!("WsChatSession - StreamHandler - handle - Nop");
            },
        }
    }
}