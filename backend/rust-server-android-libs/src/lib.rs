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

mod server;
mod session;
mod connect;
mod model;


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

pub struct ServerCollection {
    chat_server: Option<Addr<server::ChatServer>>,
    connect_server: Option<Addr<connect::connect_server::ConnectServer>>
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_startSever() {
    Runtime::new().unwrap().block_on(start_server())
}

async fn start_server() {
    async {
        let app_state = Arc::new(AtomicUsize::new(0));
        let server_collection = ServerCollection {
            chat_server: None,
            connect_server: None,
        };

        let chat_server_addr: Addr<server::ChatServer> = server::ChatServer::new(app_state.clone(), &server_collection).start();
        let connect_server_addr = connect::connect_server::ConnectServer::new(&server_collection).start();

        HttpServer::new(move || {
            App::new()
                .app_data(web::Data::new(chat_server_addr.clone()))
                .service(web::resource("/").to(index))
                .service(web::resource("/ws").to(chat_route))
                .service(web::resource("/test").to(test))
                .app_data(web::Data::new(connect_server_addr.clone()))
                .service(web::resource("/connect").to(connect))
        })
            .workers(2)
            .bind(("0.0.0.0", 8088)).unwrap().run().await;
    }
}

async fn chat_route(
    req: HttpRequest,
    stream: web::Payload,
    srv: web::Data<Addr<server::ChatServer>>,
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