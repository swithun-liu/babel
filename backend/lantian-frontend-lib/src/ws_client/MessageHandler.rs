use crate::ffi::{ClientReceiver, ClientReceiverImpl, ClientReceiverJavaImpl};

pub fn handle_ws_server_text_message(message: String, client_receiver: &ClientReceiverImpl) {
    match client_receiver {
        ClientReceiverImpl::Java(r) => {
            r.send_server_text(0, message);
        }
    }
}