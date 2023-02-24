#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::objects::{JClass, JString};
use jni::{JavaVM, JNIEnv};

use std::{
    io::stdout,
    sync::{atomic::AtomicUsize, Arc},
    thread::{self, sleep},
    time::{Duration, Instant},
};

use actix::{Actor, Addr};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web::rt::Runtime;
use actix_web_actors::ws;
use jni::sys::{JavaVMInitArgs, jstring};
use thread_priority::{ThreadBuilderExt, ThreadPriority};

mod server;
mod session;


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
    Runtime::new().unwrap().block_on(async {
        let app_state = Arc::new(AtomicUsize::new(0));

        let chat_server = server::ChatServer::new(app_state.clone()).start();

        HttpServer::new(move || {
            App::new()
                .app_data(web::Data::new(chat_server.clone()))
                .service(web::resource("/").to(index))
                .service(web::resource("/ws").to(chat_route))
                .service(web::resource("/test").to(test))
        })
            .workers(2)
            .bind(("0.0.0.0", 8088)).unwrap().run().await;
    })
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
