extern crate alloc;
extern crate core;

use core::str::Utf8Error;
use std::sync::{Arc, Mutex};
use jni::{
    objects::{
        JClass,
        JObject
    },
    JNIEnv,
};
use jni::objects::{JIntArray, JString};
use jni::strings::JavaStr;
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE, jobject, jobjectArray};
use log::debug;
use tokio::runtime::Runtime;
use crate::basic::init_debugger;
use crate::dto::StorageType;

pub mod api;
pub mod basic;
pub mod dto;
pub mod ws_client;
pub mod ffi;

#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_connectServer(
    mut env: JNIEnv, _: JClass,
    server_ip: JString,
    callback: JObject
) -> jboolean {
    init_debugger();
    debug!("Java_com_swithun_lantian_FrontEndSDK_connectServer");

    let server_ip_str = match env.get_string(&server_ip) {
        Ok(server_ip_str) => {
            match server_ip_str.to_str() {
                Ok(server_ip_str) => {
                    server_ip_str.to_string()
                }
                Err(_) => {
                    return JNI_FALSE;
                }
            }
        }
        Err(_) => {
            return JNI_FALSE;
        }
    };

    let client_receiver = ffi::ClientReceiverImpl::Java(
        ffi::ClientReceiverJavaImpl {
            env: Arc::new(Mutex::new(env)),
            callback
        }
    );

    match Runtime::new() {
        Ok(mut rt) => {
            rt.block_on(api::connect_server(&client_receiver, server_ip_str));
            JNI_TRUE
        }
        Err(e) => {
            debug!("error: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_searchServer(
    mut env: JNIEnv,
    _: JClass,
    sub_net: JString,
) -> jobjectArray {
    init_debugger();

    let default_sub_net = String::from("192.168.0");
    let sub_net_str = env.get_string(&sub_net).map_or(default_sub_net, |s| s.to_str().unwrap().to_string());

    let ips = api::search_server(sub_net_str);
    let ips_len = ips.len();

    debug!("response # ips size {}", ips.len());

    // 找到 Java 中的 String 类
    let java_string_class = env.find_class("java/lang/String").unwrap();

    // 创建一个包含所有元素的 jobjectArray
    let array = env
        .new_object_array(ips_len as i32, java_string_class, JObject::null())
        .unwrap();

    // 遍历 Vec 中的所有字符串，将它们转换为 Java 中的 String，并将它们添加到 jobjectArray 中
    for (i, s) in ips.iter().enumerate() {
        let java_string = env.new_string(s).unwrap();
        env.set_object_array_element(&array, i as i32, java_string)
            .unwrap();
    }

    array.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_getBaseFileListOfStorage(
    mut env: JNIEnv,
    _: JClass,
    s_type: jint,
    basePath: JString,
) -> jobjectArray {
    let s_type = s_type as i32;
    let s_type = StorageType::from(s_type);
    let basePath = env.get_string(&basePath).unwrap().to_str().unwrap().to_string();


    // 找到 Java 中的 String 类
    let java_string_class = env.find_class("java/lang/String").unwrap();
    let array = env.new_object_array(0, java_string_class, JObject::null()).unwrap();
    array.into_raw()
}


#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_getStorageList(
    mut env: JNIEnv,
    _: JClass,
) -> jobjectArray {
    // 找到 Java 中的 String 类
    let java_string_class = env.find_class("java/lang/String").unwrap();
    let array = env.new_object_array(0, java_string_class, JObject::null()).unwrap();
    array.into_raw()
}
