mod message_handler;

use std::io::{Read, Write};
use std::net::ToSocketAddrs;
use std::process::Command;
use std::time::Duration;
use futures_util::{SinkExt, StreamExt};
use futures_util::future::join_all;
use futures_util::stream::{Next, SplitStream};
use jni::objects::JObject;
use jni::sys::jobject;
use log::debug;
use tokio_tungstenite::{connect_async, MaybeTlsStream, WebSocketStream};
use crate::basic::init_debugger;
use tokio::io::Split;
use tokio::net::TcpStream;
use tokio::task::futures;
use tokio_tungstenite::tungstenite::handshake::client::Response;
use tokio_tungstenite::tungstenite::Message;
use crate::ws_client::message_handler::handle_ws_server_text_message;

pub(crate) async fn connect_server(client_receiver: &crate::ffi::ClientReceiverImpl<'_>, server_ip_str: String) {
    let uri = format!("ws://{}:8088/ws", server_ip_str);

    let ws_stream: Result<(WebSocketStream<MaybeTlsStream<TcpStream>>, Response), tokio_tungstenite::tungstenite::Error> = connect_async(uri).await;

    match ws_stream {
        Ok((ws, _response)) => {
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

pub(crate) fn search_server(sub_net: String) -> Vec<String> {
    let rt = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(50)
        .enable_all()
        .build()
        .unwrap();

    rt.block_on(async {
        scan_network(sub_net).await
    })
}

async fn scan_network(sub_net: String) -> Vec<String> {
    let mut tasks = vec![];

    for i in 0..=255 {
        let ip = format!("{}.{}", sub_net, i);
        debug!("scan_network #  {}", ip);
        let clone_ip = ip.clone();

        tasks.push(tokio::spawn(async move {
            let output = Command::new("ping")
                .args(["-c", "1", "-W", "1", &ip])
                .output()
                .expect("Failed to execute command");
            let stdout = String::from_utf8_lossy(&output.stdout);
            if stdout.contains("1 received") {
                if is_server_available(&clone_ip.as_str()).await {
                    debug!("scan_network # add {}", clone_ip);
                    return Some(clone_ip);
                }
            }
            None
        }));
    }

    let results = join_all(tasks).await;

    let mut available_ips = Vec::new();
    for res in results {
        if let Some(ip) = res.unwrap() {
            available_ips.push(ip)
        }
    }

    available_ips
}

async fn is_server_available(ip: &str) -> bool {
    let host = format!("{}:8088", ip);
    let addr_iter_result: Result<std::vec::IntoIter<std::net::SocketAddr>, std::io::Error> =
        host.to_socket_addrs();
    return match addr_iter_result {
        Ok(mut addr_iter) => match addr_iter.next() {
            Some(addr) => {
                debug!("check {}", host);
                let stream_result =
                    std::net::TcpStream::connect_timeout(&addr, Duration::from_millis(600));
                match stream_result {
                    Ok(mut stream) => {
                        debug!("is_server_available get stream");

                        let request = format!(
                            "GET /test HTTP/1.1\r\nHost: {}\r\nConnection: close\r\n\r\n",
                            ip
                        );
                        debug!("is_server_available get stream 2");
                        stream.write(request.as_bytes()).unwrap_or(0);
                        debug!("is_server_available get stream 3");

                        let mut response = String::new();
                        stream.read_to_string(&mut response).unwrap();
                        debug!("is session available : {:?}", response);

                        if response.contains("i am session") {
                            true
                        } else {
                            false
                        }
                    }
                    Err(e) => {
                        debug!("is_server_available err 3 {:?}", e);
                        false
                    }
                }
            }
            None => {
                debug!("is_server_available err 1");
                false
            }
        },
        Err(e) => {
            debug!("is_server_available err 2: {}", e);
            false
        }
    };
}
