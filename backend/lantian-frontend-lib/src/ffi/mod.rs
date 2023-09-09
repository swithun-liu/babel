use std::sync::{Arc, Mutex};
use jni::JNIEnv;
use jni::objects::{JString, JValue};
use log::debug;
use crate::dto::OptionCode;

pub trait ClientReceiver {
    fn call_back(&self, op: OptionCode, text: &str);
}

// java版本
pub struct ClientReceiverJavaImpl<'a> {
    pub env: Arc<Mutex<JNIEnv<'a>>>,
    pub callback: jni::objects::JObject<'a>,
}

impl<'a> ClientReceiver for ClientReceiverJavaImpl<'a> {

    fn call_back(&self, op: OptionCode, text: &str) {
        let mut env = self.env.lock().unwrap();

        let java_str: JString = env.new_string(text).unwrap_or(JString::default());
        let java_str_j_value = JValue::Object(java_str.as_ref());

        let op_str: JString = env.new_string((op as i64).to_string().as_str()).unwrap_or(JString::default());
        let op_str_j_value = JValue::Object(op_str.as_ref());

        env.call_method(&self.callback.as_ref(), "result", "(Ljava/lang/String;Ljava/lang/String;)V", &[op_str_j_value, java_str_j_value]).unwrap();
    }
}

pub enum ClientReceiverImpl<'a> {
    Java(ClientReceiverJavaImpl<'a>),
}

impl<'a> ClientReceiver for ClientReceiverImpl<'a> {

    fn call_back(&self, op: OptionCode, text: &str) {
        match self {
            ClientReceiverImpl::Java(r) => r.call_back(op, text)
        }
    }
}