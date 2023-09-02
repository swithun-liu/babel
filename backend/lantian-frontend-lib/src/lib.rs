extern crate alloc;

use std::sync::{Arc, Mutex};
use jni::{
    objects::{
        JClass,
        JObject
    },
    JNIEnv,
};
use log::debug;
use tokio::runtime::Runtime;
use crate::basic::init_debugger;

pub mod api;
pub mod basic;
pub mod dto;
pub mod ws_client;
pub mod ffi;

#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_connectServer(mut env: JNIEnv, _: JClass, callback: JObject) {
    init_debugger();

    debug!("Java_com_swithun_lantian_FrontEndSDK_connectServer");

    env.call_method(callback.as_ref(), "result", "()V", &[]).unwrap();


    let client_receiver = ffi::ClientReceiverImpl::Java(
        ffi::ClientReceiverJavaImpl {
            env: Arc::new(Mutex::new(env)),
            callback
        }
    );

    let rt = Runtime::new();
        match rt {
        Ok(mut rt) => {
            rt.block_on(api::connect_server(&client_receiver));
        }
        Err(e) => {
            debug!("error: {}", e);
        }
    }
}
