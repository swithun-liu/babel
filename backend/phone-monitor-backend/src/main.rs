use std::{
    io::stdout,
    sync::{atomic::AtomicUsize, Arc},
    thread::{self, sleep},
    time::{Duration, Instant},
};

#[macro_use]
extern crate lazy_static;

use actix::{Actor, Addr};
use actix_files::NamedFile;
use actix_web::{web, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web_actors::ws;
use device_query::{DeviceQuery, DeviceState, Keycode};
use device_query::Keycode::O;
use thread_priority::{ThreadBuilderExt, ThreadPriority};
use crate::server::ChatServer;

mod server;
mod session;

lazy_static! {
    static ref MY_SERVER: Addr<ChatServer> = {
        let app_state = Arc::new(AtomicUsize::new(0));
        server::ChatServer::new(app_state.clone()).start()
    };
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {

    // let server = server::ChatServer::new(app_state.clone()).start();
    let server_b = MY_SERVER.clone();

    std::thread::Builder::new()
        .name("keyboard".to_owned())
        .spawn_with_priority(ThreadPriority::Min, |_result| test(server_b))
        .unwrap();

    let temp = HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(MY_SERVER.clone()))
            .service(web::resource("/").to(index))
            .service(web::resource("/ws").to(chat_route))
    })
        .workers(2)
        .bind(("0.0.0.0", 8088)).unwrap().run().await;
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

async fn index() -> impl Responder {
    NamedFile::open_async("./static/index.html").await.unwrap()
}

// cmd
// cmd + c
// cmd
// cmd + c
// cmd
// cmd + c

fn test(server: Addr<server::ChatServer>) {
    let device_state = DeviceState::new();

    let mut pre_keys = vec![];
    let mut time = 0;
    let mut is_cmd = false;

    loop {
        sleep(Duration::from_millis(100));

        let new_keys: Vec<Keycode> = device_state.get_keys();

        if check_is_new_key_combination(&pre_keys, &new_keys) {
            pre_keys = new_keys.clone();
            if !is_cmd {
                // last time press cmd + c or else
                if new_keys.len() == 1 && new_keys[0] == Keycode::Meta {
                    // this time should be cmd
                    println!("cmd - time{}", time);
                    time += 1;
                    is_cmd = true;
                } else {
                    time = 0;
                    is_cmd = false;
                }
            } else {
                // last time press cmd
                if new_keys.len() == 2 {
                    // this time should be cmd + c
                    if new_keys[0] == Keycode::C && new_keys[1] == Keycode::Meta {
                        // cmd + c
                        println!("cmd + c - time{}", time);
                        time += 1;
                        is_cmd = false;
                        if time >= 6 {
                            println!("success");
                            time = 0;
                            is_cmd = false;
                            if let Ok(contents) = cli_clipboard::get_contents() {
                                server.do_send(server::ClientMessage {
                                    id: 0,
                                    msg: contents,
                                })
                            }
                        }
                    } else {
                        time = 0;
                        is_cmd = false;
                    }
                } else {
                    time = 0;
                    is_cmd = false;
                }
            }
        }
    }
}

fn check_is_new_key_combination(pre_keys: &Vec<Keycode>, new_keys: &Vec<Keycode>) -> bool {
    if pre_keys.len() != new_keys.len() {
        return true;
    }
    for i in 0..pre_keys.len() {
        if pre_keys[i] != new_keys[i] {
            return true;
        }
    }
    return false;
}
