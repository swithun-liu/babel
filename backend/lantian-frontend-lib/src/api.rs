use log::debug;
use tokio::io::Split;
use tokio::net::TcpStream;
use tokio_tungstenite::tungstenite::handshake::client::Response;
use tokio_tungstenite::tungstenite::{Error, Message};
use crate::dto::StorageType;

use crate::ws_client;

pub(crate) async fn connect_server(client_receiver: &crate::ffi::ClientReceiverImpl<'_>, server_ip_str: String) {
    ws_client::connect_server(client_receiver, server_ip_str).await;
}

pub(crate) fn search_server(sub_net: String) -> Vec<String> {
    debug!("api # search_server");
    ws_client::search_server(sub_net)
}

pub(crate) fn get_base_file_list_of_storage(s_type: StorageType, base_path: String) -> Vec<String> {
    ws_client::get_base_file_list_of_storage(s_type, base_path)
}


