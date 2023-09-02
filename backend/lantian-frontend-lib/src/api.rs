use alloc::ffi;
use futures_util::{SinkExt, StreamExt};
use futures_util::stream::{Next, SplitStream};
use log::debug;
use tokio_tungstenite::{connect_async, MaybeTlsStream, WebSocketStream};
use crate::basic::init_debugger;
use tokio::io::Split;
use tokio::net::TcpStream;
use tokio_tungstenite::tungstenite::handshake::client::Response;
use tokio_tungstenite::tungstenite::{Error, Message};

use crate::ws_client;

pub(crate) async fn connect_server(client_receiver: &crate::ffi::ClientReceiverImpl<'_>) {
    ws_client::connect_server(client_receiver).await;
}

