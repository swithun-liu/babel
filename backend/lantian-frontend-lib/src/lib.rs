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
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE, jobject, jobjectArray, jstring};
use log::debug;
use reqwest::Url;
use serde_json::Value;
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
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_getFileListOfStorage(
    mut env: JNIEnv,
    _: JClass,
    s_type: jint,
    basePath: JString,
) -> jstring {
    let s_type = s_type as i32;
    let s_type = StorageType::from(s_type);
    let basePath = env.get_string(&basePath).unwrap().to_str().unwrap().to_string();
    let query_type = match s_type {
        StorageType::LOCAL_INNER => {
            "local_inner"
        }
        StorageType::X_EXPLORE => {
            "x_explore"
        }
    };

    let mut base_url = Url::parse("http://192.168.31.249:8088/get-file-list");
    match base_url {
        Ok(mut base_url) => {
            base_url.query_pairs_mut()
                .append_pair("type", query_type)
                .append_pair("parent", basePath.as_str());

            debug!("getFileListOfStorage # base_url: {:?}", base_url);

            let result = reqwest::blocking::get(base_url);
            match result {
                Ok(r) => {
                    let json_str = r.text();
                    match json_str {
                        Ok(json_str) => {
                            env.new_string(json_str).unwrap().into_raw()
                        }
                        Err(e) => {
                            debug!("getFileListOfStorage # error2: {}", e);
                            env.new_string("").unwrap().into_raw()
                        }
                    }
                }
                Err(e) => {
                    debug!("getFileListOfStorage # error: {}", e);
                    env.new_string("").unwrap().into_raw()
                }
            }
        }
        Err(e) => {
            debug!("getFileListOfStorage # error1: {}", e);
            env.new_string("").unwrap().into_raw()
        }
    }

}


#[no_mangle]
pub extern "C" fn Java_com_swithun_lantian_FrontEndSDK_getStorageList(
    mut env: JNIEnv,
    _: JClass,
) -> jobjectArray {
    debug!("getStorage");

    // 找到 Java 中的 String 类
    let java_string_class = env.find_class("java/lang/String").unwrap();


    // http请求 http://192.168.31.249:8088/get-storage-list，返回Storage列表的json，[ Storage, Storage2]，解析成Vec<String>, 然后放到array中
    let mut storage_list: Vec<String> = vec![];
    let result = reqwest::blocking::get("http://192.168.31.249:8088/get-storage-list");
    match result {
        Ok(result) => {
            let json_str = result.text();
            match json_str {
                Ok(json_str) => {
                    let data: Vec<Value> = serde_json::from_str(json_str.as_str()).unwrap();

                    // 将每个元素转换为字符串
                    let strings: Vec<String> = data.into_iter().map(|value| value.to_string()).collect();
                    storage_list = strings;
                    for s in storage_list.iter() {
                        debug!("getStorage # s: {}", s);
                    }
                }
                Err(e) => {
                    debug!("getStorage # error3: {}", e)
                }
            }

        }
        Err(e) => {
            debug!("getStorage # error: {}", e);
        }
    }


    let array = env.new_object_array(storage_list.len() as i32, java_string_class, JObject::null()).unwrap();

    for (i, s) in storage_list.iter().enumerate() {
        let java_string = env.new_string(s).unwrap();
        env.set_object_array_element(&array, i as i32, java_string)
            .unwrap();
    }

    array.into_raw()
}
