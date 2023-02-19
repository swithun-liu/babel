use std::time::{Instant, Duration};

use actix::{Addr, Actor, StreamHandler, ActorContext, AsyncContext, Handler, WrapFuture, ActorFutureExt, fut, ContextFutureSpawner};
use actix_web_actors::ws;

use crate::server;

const HEARTBEAT_INTERAL: Duration = Duration::from_secs(5);

const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

pub struct WsChatSession {
    pub id: usize,

    pub hb: Instant,

    pub name: Option<String>,

    pub chat_server: Addr<server::ChatServer>
}

impl WsChatSession {

    fn hb(&self, ctx: &mut ws::WebsocketContext<Self>) {
        ctx.run_interval(HEARTBEAT_INTERAL, |act, ctx| {
            if Instant::now().duration_since(act.hb) > CLIENT_TIMEOUT {
                println!("Websocket client heartbeat failed, disconnecting!");

                act.chat_server.do_send(server::Disconnect { id: act.id, });

                ctx.stop();

                return;
            }

            ctx.ping(b"");
        });
    }
}

impl Actor for WsChatSession {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        self.hb(ctx);

        let addr = ctx.address();
        println!("WsChatSession # Actor # started $ ");

        self.chat_server.send(server::Connect {
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
        self.chat_server.do_send(server::Disconnect { id : self.id });
        actix::Running::Stop
    }

}

impl Handler<server::SessionMessage> for WsChatSession {
    type Result = ();

    fn handle(&mut self, msg: server::SessionMessage, ctx: &mut Self::Context) -> Self::Result {
        ctx.text(msg.0)
    }

}

impl StreamHandler<Result<ws::Message, ws::ProtocolError>> for WsChatSession {

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
                println!("WsChatSession - StreamHandler - handle - Ping");
                ctx.pong(&msg);
            },
            ws::Message::Pong(_) => {
                println!("WsChatSession - StreamHandler - handle - Pong");
                self.hb = Instant::now();
            },
            ws::Message::Text(text) => {
                let msg = text.trim();

                self.chat_server.do_send(server::ClientMessage {
                    id: self.id,
                    msg: msg.to_owned(),
                })
            },
            ws::Message::Binary(_) => println!("Unexpected binary"),
            ws::Message::Continuation(_) => {
                ctx.stop();
            },
            ws::Message::Close(reason) => {
                ctx.close(reason);
                ctx.stop();
            },
            ws::Message::Nop => { },
        }
    }
}