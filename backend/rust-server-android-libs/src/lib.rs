#![cfg(target_os = "android")]
#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getTestStr(
    env: JNIEnv, _: JClass,
) -> jstring {
    env.new_string("Hello World!")
        .expect("Couldn't create java string!")
        .into_inner()
}

#[no_mangle]
pub extern "C" fn Java_com_swithun_liu_ServerSDK_getTestStrWithInput(
    env: JNIEnv, _: JClass, input: JString,
) -> jstring {
    let input: String = env.get_string(input)
        .expect("Couldn't get java string!")
        .into();
    let output = env.new_string(format!("Hello, {}!", input))
        .expect("Couldn't create java string!");
    output.into_inner()
}
