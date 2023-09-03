use std::sync::{Arc, Mutex};
use jni::JNIEnv;
use jni::objects::JValue;
use log::debug;

pub trait ClientReceiver {
    fn send_server_text(&self, op: i32, message: String);
}

// java版本
pub struct ClientReceiverJavaImpl<'a> {
    pub env: Arc<Mutex<JNIEnv<'a>>>,
    pub callback: jni::objects::JObject<'a>,
}

impl<'a> ClientReceiver for ClientReceiverJavaImpl<'a> {
    fn send_server_text(&self, _op: i32, _message: String) {
        debug!("ClientReceiverJavaImpl # send_server_text");

        let mut env = self.env.lock().unwrap();

        env.call_method(&self.callback.as_ref(), "result", "()V", &[]).unwrap();
    }
}

pub enum ClientReceiverImpl<'a> {
    Java(ClientReceiverJavaImpl<'a>),
}