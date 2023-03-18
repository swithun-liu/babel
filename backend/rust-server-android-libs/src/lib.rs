#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::objects::{JClass, JObject, JString};
use jni::{JNIEnv};

use std::{env, sync::{atomic::AtomicUsize, Arc}, time::{Instant}};

use actix::{Actor, Addr};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web::rt::Runtime;
use actix_web_actors::ws;
use jni::sys::{jobject, jstring};
use lazy_static::lazy_static;
use std::collections::{HashMap, VecDeque};
use std::ffi::OsStr;
use std::fmt::format;
use std::fs::{File, read};
use std::io::{Read, Seek, SeekFrom, Write};
use std::net::{IpAddr, Ipv4Addr, ToSocketAddrs};
use std::pin::Pin;
use std::process::{Command, Output};
use std::sync::{Mutex};
use futures::channel::oneshot;
use uuid::Uuid;
use std::string::String;
use std::task::{Context, Poll};
use std::time::Duration;
use android_logger::Config;
use log::{debug, info, Level};
use crate::model::option_code;
use futures::{pin_mut, SinkExt, TryFutureExt, TryStreamExt};
use pnet::datalink::NetworkInterface;
use pnet::ipnetwork::IpNetwork;
use pnet::util::MacAddr;
use serde::{Deserialize, Serialize};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::net::unix::SocketAddr;

mod server;
mod session;
mod connect;
mod model;

use crate::model::communicate_models;

#[macro_use]
extern crate log;
extern crate android_logger;
extern crate core;

lazy_static! {
    static ref KERNEL_SERVER: Addr<connect::connect_server::ConnectServer> = {
        connect::connect_server::ConnectServer::new().start()
    };
    static ref CLIENT_SERVER: Addr<server::ClientServer> = {
        let app_state = Arc::new(AtomicUsize::new(0));
        server::ClientServer::new(app_state.clone(),  KERNEL_SERVER.clone()).start()
    };
    static ref SERVER_CLIENT_REQUEST_MAP: Arc<Mutex<HashMap<std::string::String, oneshot::Sender<std::string::String>>>> = Arc::new(Mutex::new(HashMap::new()));
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getTestStr(env: JNIEnv, _: JClass) -> jstring {
    env.new_string("Hello World!")
        .expect("Couldn't create java string!")
        .into_inner()
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getTestStrWithInput(
    env: JNIEnv,
    _: JClass,
    input: JString,
) -> jstring {
    let input: String = env
        .get_string(input)
        .expect("Couldn't get java string!")
        .into();
    let output = env
        .new_string(format!("Hello, {}!", input))
        .expect("Couldn't create java string!");
    output.into_inner()
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getAllServerInLAN(
    env: JNIEnv,
    _: JClass,
) -> jobject {
    let config = Config::default().with_min_level(Level::Debug);
    android_logger::init_once(config);
    debug!("response # {}", 1);
    let rt = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    debug!("response # {}", 2);

    let ips: Vec<String> = rt.block_on(async {
        debug!("response # {}", 3);
        scan_network().await
    });
    debug!("response # {}", 4);

    // 找到 Java 中的 String 类
    let java_string_class = env.find_class("java/lang/String").unwrap();

    // 创建一个包含所有元素的 jobjectArray
    let array = env.new_object_array(ips.len() as i32, java_string_class, JObject::null()).unwrap();

    // 遍历 Vec 中的所有字符串，将它们转换为 Java 中的 String，并将它们添加到 jobjectArray 中
    for (i, s) in ips.iter().enumerate() {
        let java_string = env.new_string(s).unwrap();
        env.set_object_array_element(array, i as i32, java_string.into_inner()).unwrap();
    }

    debug!("response # {}", 6);

    array.into()
}

async fn scan_network_2() -> Vec<String> {
    debug!("m2 # {}", 1);
    let interface_name = "eth0";
    debug!("m2 # {}", 2);
    let interfaces: Vec<NetworkInterface> = pnet::datalink::interfaces();
    debug!("interface size: {}", interfaces.len());

    for interface in interfaces {
        debug!("interface ip size : {} {:?}", &interface.ips.len(), interface);

        debug!("m2 # {}", 3);
        let target_mac = MacAddr::new(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);
        debug!("m2 # {}", 4);
        let target_ip = Ipv4Addr::new(192, 168, 0, 1);
        debug!("m2 # {}", 5);

        let source_mac = MacAddr::new(0, 0, 0, 0, 0, 0);
        let source_ip = Ipv4Addr::new(0, 0, 0, 0);

        debug!("m2 # {}", 6);
        let mut arp_buffer = [0u8; 42];
        debug!("m2 arpbuffer # {:?}", arp_buffer);
        let mut arp_packet = pnet::packet::arp::MutableArpPacket::new(&mut arp_buffer[..]).unwrap();
        arp_packet.set_hardware_type(pnet::packet::arp::ArpHardwareTypes::Ethernet);
        arp_packet.set_protocol_type(pnet::packet::ethernet::EtherTypes::Ipv4);
        arp_packet.set_hw_addr_len(6);
        arp_packet.set_proto_addr_len(4);
        arp_packet.set_operation(pnet::packet::arp::ArpOperations::Request);
        arp_packet.set_sender_hw_addr(source_mac);
        arp_packet.set_sender_proto_addr(source_ip);
        arp_packet.set_target_hw_addr(target_mac);
        arp_packet.set_target_proto_addr(target_ip);

        debug!("m2 # {}", 7);
        let mut ether_buffer = [0u8; 100];
        debug!("ether buffer1: {:?}", ether_buffer);
        let mut ether_packet = pnet::packet::ethernet::MutableEthernetPacket::new(&mut ether_buffer).unwrap();
        debug!("m2 # {}", 8);

        ether_packet.set_destination(target_mac);
        debug!("m2 # {}", 9);
        ether_packet.set_source(source_mac);
        debug!("m2 # {}", 10);
        ether_packet.set_ethertype(pnet::packet::ethernet::EtherTypes::Arp);
        debug!("m2 # {}", 11);
        debug!("m2 arpbuffer # {:?}", arp_buffer);
        ether_packet.set_payload(&arp_buffer[..]);
        debug!("m2 # {}", 12);

        debug!("ether_pakcet: {:?}", ether_packet);
        debug!("ether buffer2: {:?}", ether_buffer);

        let (mut tx, mut rx) = match pnet::datalink::channel(&interface, Default::default()) {
            Ok(pnet::datalink::Channel::Ethernet(tx, rx)) => {
                debug!("suc");
                (tx, rx)
            },
            Err(e) => {
                debug!("Failed to create datalink channel {:?}", e);
                panic!("Failed to create datalink channel");
            }
            _ => {
                debug!("Failed to create datalink channel other");
                panic!("Failed to create datalink channel");
            }
        };

        tx.send_to(&ether_buffer, Some(interface));

        while let Ok(packet) = rx.next() {
            debug!("one");
            let ether = pnet::packet::ethernet::EthernetPacket::new(packet).unwrap();
            if ether.get_ethertype() == pnet::packet::ethernet::EtherTypes::Arp {
                // let arp = pnet::arp::ArpPacket(ether.payload()).unwrap();
                debug!("IP: {:?}, ", ether);
            }
        }
    }


    //
    // debug!("m2 # {}", 3);
    // let target_mac = MacAddr::new(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);
    // debug!("m2 # {}", 4);
    // let target_ip = Ipv4Addr::new(192, 168, 0, 1);
    // debug!("m2 # {}", 5);
    //
    // let source_mac = MacAddr::new(0, 0, 0, 0, 0, 0);
    // let source_ip = Ipv4Addr::new(0, 0, 0, 0);
    //
    // debug!("m2 # {}", 6);
    // let mut arp_buffer = [0u8; 42];
    // let mut arp_packet = pnet::packet::arp::MutableArpPacket::new(&mut arp_buffer[..]).unwrap();
    // arp_packet.set_hardware_type(pnet::packet::arp::ArpHardwareTypes::Ethernet);
    // arp_packet.set_protocol_type(pnet::packet::ethernet::EtherTypes::Ipv4);
    // arp_packet.set_hw_addr_len(6);
    // arp_packet.set_proto_addr_len(4);
    // arp_packet.set_operation(pnet::packet::arp::ArpOperations::Request);
    // arp_packet.set_sender_hw_addr(source_mac);
    // arp_packet.set_sender_proto_addr(source_ip);
    // arp_packet.set_target_hw_addr(target_mac);
    // arp_packet.set_target_proto_addr(target_ip);
    //
    // debug!("m2 # {}", 7);
    // let mut ether_buffer = [0u8; 42];
    // debug!("ether buffer1: {:?}", ether_buffer);
    // let mut ether_packet = pnet::packet::ethernet::MutableEthernetPacket::new(&mut ether_buffer).unwrap();
    // debug!("m2 # {}", 8);
    //
    // ether_packet.set_destination(target_mac);
    // ether_packet.set_source(source_mac);
    // ether_packet.set_ethertype(pnet::packet::ethernet::EtherTypes::Arp);
    // ether_packet.set_payload(&arp_buffer);
    //
    // debug!("ether_pakcet: {:?}", ether_packet);
    // debug!("ether buffer2: {:?}", ether_buffer);
    //
    // let (mut tx, mut rx) = match pnet::datalink::channel(&interface, Default::default()){
    //     Ok(pnet::datalink::Channel::Ethernet(tx, rx)) => {
    //         debug!("suc");
    //         (tx, rx)
    //     },
    //     _ => {
    //         debug!("Failed to create datalink channel");
    //         panic!("Failed to create datalink channel");
    //     }
    // };
    //
    // tx.send_to(&ether_buffer, Some(interface));
    //
    // while let Ok(packet) = rx.next() {
    //     debug!("one");
    //     let ether = pnet::packet::ethernet::EthernetPacket::new(packet).unwrap();
    //     if ether.get_ethertype() == pnet::packet::ethernet::EtherTypes::Arp {
    //         // let arp = pnet::arp::ArpPacket(ether.payload()).unwrap();
    //         debug!("IP: {:?}, ", ether);
    //     }
    // }

    vec![]

}

async fn scan_network() -> Vec<String> {
    let mut tasks = vec![];

    for i in 100..=110 {
        let ip = format!("192.168.0.{}", i);
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

    let results = futures::future::join_all(tasks).await;
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
    let mut addr_iter_result = host.to_socket_addrs();
    return match addr_iter_result {
        Ok(mut addr_iter) => {
            match addr_iter.next() {
                Some(addr) => {
                    debug!("check {}", host);
                    let mut stream_result = std::net::TcpStream::connect_timeout(&addr, Duration::from_millis(600));
                    match stream_result {
                        Ok(mut stream) => {
                            debug!("is_server_available get stream");

                            let request = format!("GET /test HTTP/1.1\r\nHost: {}\r\nConnection: close\r\n\r\n", ip);
                            debug!("is_server_available get stream 2");
                            stream.write(request.as_bytes()).unwrap_or(0);
                            debug!("is_server_available get stream 3");

                            let mut response = String::new();
                            stream.read_to_string(&mut response).unwrap();
                            debug!("is server available : {:?}", response);

                            if response.contains("i am server") {
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
            }
        }
        Err(e) => {
            debug!("is_server_available err 2");
            false
        }
    };
}

async fn index() -> impl Responder {
    NamedFile::open_async("./static/index.html").await.unwrap()
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_startSever() {
    let config = Config::default().with_min_level(Level::Debug);
    android_logger::init_once(config);

    debug!("rust debug");

    Runtime::new().unwrap().block_on(
        async {
            HttpServer::new(move || {
                App::new()
                    .app_data(web::Data::new(CLIENT_SERVER.clone()))
                    .service(web::resource("/").to(index))
                    .service(web::resource("/ws").to(chat_route))
                    .service(web::resource("/test").to(test))
                    .app_data(web::Data::new(KERNEL_SERVER.clone()))
                    .service(web::resource("/connect").to(connect))
                    .service(web::resource("/get_path_list").route(web::get().to(get_path_list)))
                    .service(web::resource("/get-video").to(get_video))
            })
                .workers(2)
                .bind(("0.0.0.0", 8088)).unwrap().run().await.expect("panic");
        }
    )
}

async fn get_video(
    query: web::Query<HashMap<String, String>>,
    req: HttpRequest,
) -> Result<HttpResponse, Error> {
    let temp = "failed path".to_string();
    let path = query.get("path").unwrap_or(&temp);
    info!("get_video / {}", path);

    let file = File::open(&path)?;
    let metadata = file.metadata()?;
    let content_length = metadata.len();
    let content_type = get_content_type(&path.as_str()).unwrap_or("err");
    info!(
        "get_video # content_length: {} content-type: {}",
        content_length,
        content_type.clone()
    );

    // 打印所有请求头
    for (header_name, header_value) in req.headers().iter() {
        debug!("Header: {} => {:?}", header_name, header_value);
    }

    let range = req.headers().get("range").and_then(|header_value| {
        info!("get range: {}", header_value.clone().to_str().ok()?);
        let header_value = header_value.to_str().ok()?;
        let byte_ranges = header_value.trim_start_matches("bytes=").trim();
        info!("1");
        let mut parts = byte_ranges.split('-');
        info!("2");
        let start = parts.next()?.parse().ok()?;
        info!("3");
        let end = parts.next().map(|s| s.parse().ok()).unwrap_or(Some(content_length as usize - 1)).unwrap_or(content_length as usize - 1);
        info!("4");
        info!("start {} end {}", start, end);
        Some((start, end))
    });

    let (start, end) = match range {
        Some((start, end)) => {
            info!("get_video $ start:{} end: {}", start, end);
            if start > content_length as usize || start > end || end >= content_length as usize {
                info!("Invalid range");
                return Err(actix_web::error::ErrorBadRequest("Invalid range"));
            }
            (start, end)
        }
        None => (0, content_length as usize - 1)
    };

    let size = (end - start + 1) as u64;
    Ok(
        HttpResponse::PartialContent()
            .append_header(("Content-Type", content_type))
            .append_header(("Content-Length", content_length))
            .append_header(("Content-Range", format!("bytes {}-{}/{}", start, end, content_length)))
            .streaming(
                Box::pin(
                    FileReaderStream::new(file, start as u64)
                )
            )
    )
}

struct FileReaderStream {
    file: std::fs::File,
    pos: u64,
}

impl FileReaderStream {
    fn new(file: std::fs::File, pos: u64) -> FileReaderStream {
        FileReaderStream {
            file,
            pos,
        }
    }
}

impl futures::Stream for FileReaderStream {
    type Item = Result<actix_web::web::Bytes, actix_web::Error>;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let mut file = &self.file;
        let mut buf = vec![0u8; 1024 * 1024];

        file.seek(SeekFrom::Start(self.pos as u64))?;

        let n = match file.read(&mut buf) {
            Ok(n) => n,
            Err(ref e) if e.kind() == std::io::ErrorKind::Interrupted => return Poll::Pending,
            Err(e) => return Poll::Ready(Some(Err(actix_web::Error::from(e))))
        };

        if n == 0 {
            return Poll::Ready(None);
        }

        let bytes = actix_web::web::Bytes::copy_from_slice(&buf[..n]);
        self.pos += n as u64;

        Poll::Ready(Some(Ok(bytes)))
    }
}

fn get_content_type(file_path: &str) -> Option<&'static str> {
    match file_path.split('.').last() {
        Some("html") => Some("text/html"),
        Some("css") => Some("text/css"),
        Some("js") => Some("application/javascript"),
        Some("json") => Some("application/json"),
        Some("jpg") | Some("jpeg") => Some("image/jpeg"),
        Some("png") => Some("image/png"),
        Some("gif") => Some("image/gif"),
        Some("bmp") => Some("image/bmp"),
        Some("ico") => Some("image/x-icon"),
        Some("pdf") => Some("application/pdf"),
        Some("mp4") => Some("video/mp4"),
        Some("mov") => Some("video/quicktime"),
        Some("webm") => Some("video/webm"),
        Some("wmv") => Some("video/x-ms-wmv"),
        Some("mkv") => Some("video/x-matroska"),
        _ => None
    }
}

async fn get_path_list(
    query: web::Query<HashMap<String, String>>
) -> impl Responder {
    let path_option = query.get("path");

    debug!("rust get_path_list/{}", path_option.clone().unwrap());

    match path_option {
        Some(path) => {
            match path.as_str() {
                "base" => {
                    debug!("get_path_list: base_string, {}", option_code::OptionCode::CommonOptionCode::GET_BASE_PATH_LIST_REQUEST as i32);
                    let new_uuid: std::string::String = format!("{}", Uuid::new_v4());
                    let (tx, rx) = oneshot::channel();

                    SERVER_CLIENT_REQUEST_MAP.lock().unwrap().insert(new_uuid.clone(), tx);
                    let json_struct = communicate_models::CommonCommunicateJsonStruct {
                        uuid: new_uuid,
                        code: option_code::OptionCode::CommonOptionCode::GET_BASE_PATH_LIST_REQUEST as i32,
                        content: "".to_string(),
                        content_type: 0,
                    };
                    kernel_send_message_to_front_end(json_struct);

                    match rx.await {
                        Ok(result) => HttpResponse::Ok().body(result),
                        Err(_) => HttpResponse::InternalServerError().finish(),
                    }
                }
                _ => {
                    debug!("get_path_list: {} code {}", path, option_code::OptionCode::CommonOptionCode::GET_CHILDREN_PATH_LIST_REQUEST as i32);

                    let new_uuid: std::string::String = format!("{}", Uuid::new_v4());
                    let (tx, rx) = oneshot::channel();

                    SERVER_CLIENT_REQUEST_MAP.lock().unwrap().insert(new_uuid.clone(), tx);
                    let json_struct = communicate_models::CommonCommunicateJsonStruct {
                        uuid: new_uuid,
                        code: option_code::OptionCode::CommonOptionCode::GET_CHILDREN_PATH_LIST_REQUEST as i32,
                        content: path.to_string(),
                        content_type: 0
                    };
                    kernel_send_message_to_front_end(json_struct);

                    match rx.await {
                        Ok(result) => HttpResponse::Ok().body(result),
                        Err(_) => HttpResponse::InternalServerError().finish(),
                    }
                }
            }
        }
        None => {
            HttpResponse::Ok().body("err 1".to_string())
        }
    }
}

async fn chat_route(
    req: HttpRequest,
    stream: web::Payload,
    srv: web::Data<Addr<server::ClientServer>>,
) -> Result<HttpResponse, Error> {
    let session = session::WsChatSession {
        id: 0,
        hb: Instant::now(),
        name: None,
        chat_server: srv.get_ref().clone(),
    };

    ws::start(session, &req, stream)
}

async fn test() -> String {
    debug!("somebody test");
    "i am server".to_string()
}

async fn connect(
    req: HttpRequest,
    stream: web::Payload,
    srv: web::Data<Addr<connect::connect_server::ConnectServer>>,
) -> Result<HttpResponse, Error> {
    let session = connect::connect_session::ConnectSession {
        hb: Instant::now(),
        connect_server: srv.get_ref().clone(),
    };

    ws::start(session, &req, stream)
}

pub fn kernel_send_message_to_front_end(json_struct: communicate_models::CommonCommunicateJsonStruct) {
    let json_struct_str = serde_json::to_string(&json_struct).unwrap();

    KERNEL_SERVER.do_send(connect::connect_server::KernelToFrontEndMessage {
        msg: json_struct_str,
    })
}

pub fn handle_android_front_end_response(kernel_and_front_end_json: communicate_models::CommonCommunicateJsonStruct) {
    let uuid = kernel_and_front_end_json.uuid;
    let result = kernel_and_front_end_json.content;
    let mut request_map = SERVER_CLIENT_REQUEST_MAP.lock().unwrap();
    match request_map.remove(&uuid) {
        Some(tx) => {
            let _ = tx.send(result);
        }
        None => {
            eprintln!("Failed to find request with uuid: {}", uuid);
        }
    }
}