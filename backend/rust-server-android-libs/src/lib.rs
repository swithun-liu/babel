#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::objects::{JClass, JString};
use jni::{JNIEnv};

use std::{
    sync::{atomic::AtomicUsize, Arc},
    time::{Instant},
};

use actix::{Actor, Addr, ContextFutureSpawner};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web::rt::Runtime;
use actix_web_actors::ws;
use jni::sys::{jstring};
use lazy_static::lazy_static;
use std::collections::HashMap;
use std::fs::File;
use std::io::{BufReader, Read, Seek, SeekFrom};
use std::pin::Pin;
use std::sync::{Mutex};
use futures::channel::oneshot;
use uuid::Uuid;
use std::string::String;
use std::task::{Context, Poll};
use actix::fut::ok;
use actix_web::body::BodyStream;
use actix_web::cookie::time::format_description::parse;
use actix_web::dev::JsonBody::Body;
use actix_web::error::ParseError;
use actix_web::http::header::Range;
use actix_web::web::Bytes;
use android_logger::Config;
use log::{debug, error, info, Level, LevelFilter};
use crate::model::option_code;
use futures::{SinkExt, Stream, TryFutureExt, TryStreamExt};
use futures::stream::once;
use tokio::sync::mpsc;

mod server;
mod session;
mod connect;
mod model;

use crate::model::communicate_models;
#[macro_use] extern crate log;
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
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getTestStr(env: JNIEnv, _: JClass) -> jstring{
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
    req: HttpRequest
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
                return Err(actix_web::error::ErrorBadRequest("Invalid range"))
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
            return Poll::Ready(None)
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
    "haha".to_string()
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
        },
        None => {
            eprintln!("Failed to find request with uuid: {}", uuid);
        }
    }
}