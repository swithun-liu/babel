mod MessageHandler;

use futures_util::{SinkExt, StreamExt};
use futures_util::stream::{Next, SplitStream};
use log::debug;
use tokio_tungstenite::{connect_async, MaybeTlsStream, WebSocketStream};
use crate::basic::init_debugger;
use tokio::io::Split;
use tokio::net::TcpStream;
use tokio_tungstenite::tungstenite::handshake::client::Response;
use tokio_tungstenite::tungstenite::{Error, Message};
use crate::ws_client::MessageHandler::handle_ws_server_text_message;

pub(crate) async fn connect_server(client_receiver: &crate::ffi::ClientReceiverImpl<'_>) {

    let ws_stream: Result<(WebSocketStream<MaybeTlsStream<TcpStream>>, Response), tokio_tungstenite::tungstenite::Error> = connect_async("ws://192.168.31.249:8088/ws").await;

    match ws_stream {
        Ok((ws, response)) => {
            let (mut write, mut read) = ws.split();
            // 发送消息"Hello WebSocket"

            let send_result = write.send(Message::Text(
                r#"
             {
                "event": "ping",
                "reqid": 42
             }
             "#.to_string() + "\n")).await;
            match send_result {
                Err(e) => {
                    // 打印日志
                    debug!("WS_CLIENT write send error: {}", e);
                }
                _ => { }
            };
            while let Some(message) = read.next().await {
                match message {
                    Ok(Message::Text(text)) => {
                        debug!("WS_CLIENT receive message: {}", text);
                        handle_ws_server_text_message(text, client_receiver)
                    }
                    Ok(Message::Binary(_)) => {
                        debug!("WS_CLIENT receive message: binary")
                    }
                    Ok(Message::Ping(_)) => {
                        debug!("WS_CLIENT receive message: ping")
                    }
                    Ok(Message::Pong(_)) => {
                        debug!("WS_CLIENT receive message: pong")
                    }
                    Ok(Message::Close(_)) => {
                        debug!("WS_CLIENT receive message: close")
                    }
                    Ok(Message::Frame(_)) => {
                        debug!("WS_CLIENT receive message: frame")
                    }
                    Err(_) => {}
                }
            }
        }
        Err(e) => {
            // 打印日志
            debug!("WS_CLIENT connect ws error: {}", e);
        }
    };
}

