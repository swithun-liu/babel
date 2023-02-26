#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::objects::{JClass, JString};
use jni::{JNIEnv};

use std::{
    sync::{atomic::AtomicUsize, Arc},
    time::{Instant},
};
use std::ops::Add;

use actix::{Actor, Addr};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web::rt::Runtime;
use actix_web_actors::ws;
use jni::sys::{jstring};
use lazy_static::lazy_static;
use std::collections::HashMap;
use std::fmt::format;
use std::sync::Mutex;
use futures::channel::oneshot;
use uuid::Uuid;
use std::string::String;

#[macro_use] extern crate log;
extern crate android_logger;

mod server;
mod session;
mod connect;
mod model;

use crate::model::communicate_models;

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

    // Initialize android_logger
    // env_logger::builder().filter_level(log::LevelFilter::Trace).init();
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Trace).with_tag("myrust")
    );

    info!("rust test");

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
                    .service(web::resource("/get_path_list")
                        .route(web::get().to(get_path_list))

                    )
            })
                .workers(2)
                .bind(("0.0.0.0", 8088)).unwrap().run().await.expect("panic");
        }
)
}

async fn get_path_list(
    query: web::Query<HashMap<String, String>>
) -> impl Responder {
    let key_option = query.get("path");

    kernel_send_message_to_front_end(communicate_models::CommonCommunicateJsonStruct {
        uuid: "".to_string(),
        code: 0,
        content: "get_path_list".to_string(),
    });

    match key_option {
        Some(key) => {
            let base_string = String::from("base");

            kernel_send_message_to_front_end(communicate_models::CommonCommunicateJsonStruct {
                uuid: "".to_string(),
                code: 0,
                content: "suc".to_string(),
            });

            match key {
                base_string => {
                    let new_uuid: std::string::String = format!("{}", Uuid::new_v4());
                    let (tx, rx) = oneshot::channel();

                    SERVER_CLIENT_REQUEST_MAP.lock().unwrap().insert(new_uuid.clone(), tx);
                    let json_struct = communicate_models::CommonCommunicateJsonStruct {
                        uuid: new_uuid,
                        code: 1,
                        content: "".to_string(),
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
            kernel_send_message_to_front_end(communicate_models::CommonCommunicateJsonStruct {
                uuid: "".to_string(),
                code: 0,
                content: "err 1".to_string(),
            });
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