use std::time::{Instant, Duration};

use actix::{Addr, Actor, StreamHandler, ActorContext, AsyncContext, Handler, WrapFuture, ActorFutureExt, fut, ContextFutureSpawner};
use actix_web_actors::ws;
use log::debug;

const HEARTBEAT_INTERVAL: Duration = Duration::from_secs(5);
const CLIENT_TIMEOUT: Duration = Duration::from_secs(10);

use crate::connect::connect_server;

pub struct ConnectSession {
    pub hb: Instant,
    pub connect_server: Addr<connect_server::ConnectServer>
}

impl ConnectSession {
    fn hb(&self, ctx: &mut ws::WebsocketContext<Self>) {
        ctx.run_interval(HEARTBEAT_INTERVAL, |act, ctx| {
            if Instant::now().duration_since(act.hb) > CLIENT_TIMEOUT {
                debug!("Websocket client heartbeat failed, disconnecting!");
                ctx.stop();
                return;
            }
            ctx.ping(b"");
        });
    }
}

impl Actor for ConnectSession {
    type Context = ws::WebsocketContext<Self>;
    fn started(&mut self, ctx: &mut Self::Context) {
        self.hb(ctx);

        let addr = ctx.address();

        self.connect_server.send(connect_server::FronterConnectMessage{
            addr: addr.recipient(),
        })
            .into_actor(self)
            .then(|res, act, ctx| {
                match res {
                    Ok(res) => {
                    }
                    Err(_) => {
                        ctx.stop();
                    }
                }
                fut::ready(())
            }).wait(ctx);
    }

    fn stopping(&mut self, ctx: &mut Self::Context) -> actix::Running {
        actix::Running::Stop
    }
}

impl Handler<connect_server::ConnectSessionMessage> for ConnectSession {
    type Result = ();

    fn handle(&mut self, msg: crate::connect::connect_server::ConnectSessionMessage, ctx: &mut Self::Context) -> Self::Result {
        ctx.text(msg.0)
    }
}

impl StreamHandler<Result<ws::Message, ws::ProtocolError>> for ConnectSession {

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
                debug!("swithun-xxxx # rust # ConnnectSession - handle - Ping");
                ctx.pong(&msg);
            }
            ws::Message::Pong(msg) => {
                debug!("swithun-xxxx # rust # ConnnectSession - handle - Pong");
                self.hb = Instant::now();
            }
            ws::Message::Text(text) => {
                let msg = text.trim();
                debug!("swithun-xxxx # rust # ConnnectSession - handle - Text {}", msg);
                self.connect_server.do_send(connect_server::FronterMessage{
                    msg: msg.to_owned(),
                });

            }
            ws::Message::Binary(_) => {
                debug!("swithun-xxxx # rust # ConnnectSession - handle - Binary");
            }
            ws::Message::Close(reason) => {
                ctx.close(reason);
                ctx.stop();
            }
            ws::Message::Nop => {

            }
            ws::Message::Continuation(_) => {

            }
        }
    }
}
