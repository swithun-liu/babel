#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::objects::{JClass, JString};
use jni::{JNIEnv};

use std::{
    sync::{atomic::AtomicUsize, Arc},
    time::{Instant},
};

use actix::{Actor, Addr};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web::rt::Runtime;
use actix_web_actors::ws;
use jni::sys::{jstring};
use lazy_static::lazy_static;
use std::collections::HashMap;
use std::fs::File;
use std::io::Read;
use std::sync::Mutex;
use futures::channel::oneshot;
use uuid::Uuid;
use std::string::String;
use std::task::Poll;
use android_logger::Config;
use log::{debug, error, info, Level, LevelFilter};
use crate::model::option_code;
use futures::{TryFutureExt, TryStreamExt};

mod server;
mod session;
mod connect;
mod model;

use crate::model::communicate_models;
#[macro_use] extern crate log;
extern crate android_logger;

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
    query: web::Query<HashMap<String, String>>
) -> Result<HttpResponse, Error> {
    let temp = "failed path".to_string();
    let path = query.get("path").unwrap_or(&temp);
    debug!("get_video / {}", path);

    let file = File::open(&path)?;
    let metadata = file.metadata()?;
    let file_size = metadata.len();

    let response = HttpResponse::Ok()
        .header("Content-Type", "video/x-matroska")
        .header("Content-Length", file_size.to_string())
        .streaming(futures::stream::poll_fn(move |ctx| {
            let mut buffer = [0; 64 * 1024];
            let mut reader = std::io::BufReader::new(file.try_clone()?);

            match reader.read(&mut buffer) {
                Ok(0) => Poll::Ready(None),
                Ok(n) => {
                    let chunk = actix_web::web::Bytes::from(buffer[..n].to_owned().into_boxed_slice());
                    Poll::Ready(Some(Ok(chunk)))
                }
                Err(e) if e.kind() == std::io::ErrorKind::Interrupted => Poll::Pending,
                Err(e) => Poll::Ready(Some(Err(e))),
            }
        }));

    Ok(response)

}

// fn content_type_by_extension<P: AsRef<>>(path: P) -> Option<&'static str> {
//     match path.as_ref().extension().and_then(|e| e.to_str()) {
//         Some("mp4") => Some("video/mp4"),
//         Some("mkv") => Some("video/x-matroska"),
//         Some("avi") => Some("video/x-msvideo"),
//         Some("webm") => Some("video/webm"),
//         _ => None,
//     }
// }

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