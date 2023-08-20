use std::fs::File;
use std::future::{Future, poll_fn};
use std::pin::Pin;
use std::task::{Context, Poll};
use std::io::{Read, Seek, SeekFrom};
use std::thread::sleep;
use std::time::Duration;
use actix::dev::Stream;
use actix_web::Error;
use actix_web::web::Bytes;
use futures::channel::oneshot;
use futures::channel::oneshot::{Canceled, Receiver, Sender};
use futures::TryStreamExt;
use log::debug;
use uuid::Uuid;
use crate::{get_content_type, kernel_send_message_to_front_end, message_kernel_to_front_end};
use crate::model::{communicate_models, option_code};

pub trait FileStream: futures::Stream<Item=Result<actix_web::web::Bytes, actix_web::Error>> + Unpin {
    fn get_file_length(&self) -> u64;
    fn get_file_type(&self) -> String;
}

pub struct LocalFileStream {
    file: File,
    content_length: u64,
    content_type: String,
    pos: u64,
}

pub struct AndroidUsbStorageFileStream {
    path: String,
    content_length: u64,
    content_type: String,
    pos: u64,
}

impl LocalFileStream {
    pub fn new(path: &str, pos: u64) -> LocalFileStream {
        let file = File::open(&path).unwrap();
        let metadata = file.metadata().unwrap();
        let content_length = metadata.len();
        let content_type = get_content_type(path).unwrap_or("err");

        LocalFileStream {
            file: file,
            content_length: content_length,
            content_type: content_type.to_string(),
            pos: pos
        }
    }
}

impl AndroidUsbStorageFileStream {
    pub async fn new(path: &str, pos: u64, content_length: u64) -> AndroidUsbStorageFileStream {

        let content_type = get_content_type(path).unwrap_or("err");

        AndroidUsbStorageFileStream {
            path: path.to_string(),
            content_length: content_length,
            content_type: content_type.to_string(),
            pos: pos,
        }
    }
}

impl FileStream for LocalFileStream {
    fn get_file_length(&self) -> u64 {
        return self.content_length
    }

    fn get_file_type(&self) -> String {
        return self.content_type.clone()
    }
}

impl FileStream for AndroidUsbStorageFileStream {
    fn get_file_length(&self) -> u64 {
        self.content_length
    }

    fn get_file_type(&self) -> String {
        self.content_type.clone()
    }
}

impl futures::Stream for LocalFileStream {
    type Item = Result<actix_web::web::Bytes, actix_web::Error>;

    fn poll_next(mut self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let mut file = &self.file;
        let mut buf = vec![0u8; 1024 * 1024];

        file.seek(SeekFrom::Start(self.pos as u64))?;

        let n = match file.read(&mut buf) {
            Ok(n) => n,
            Err(ref e) if e.kind() == std::io::ErrorKind::Interrupted => return Poll::Pending,
            Err(e) => return Poll::Ready(Some(Err(actix_web::Error::from(e)))),
        };

        if n == 0 {
            return Poll::Ready(None);
        }

        let bytes = actix_web::web::Bytes::copy_from_slice(&buf[..n]);
        self.pos += n as u64;

        Poll::Ready(Some(Ok(bytes)))
    }
}


impl Stream for AndroidUsbStorageFileStream {
    type Item = Result<actix_web::web::Bytes, actix_web::Error>;

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        debug!("AndroidUsbStorageFileStream pull_next");

        let new_uuid: String = format!("{}", Uuid::new_v4());
        let content = format!("{};{}", self.pos.to_string(), self.path.clone());

        debug!("AndroidUsbStorageFileStream pull_next {}", new_uuid);
        let json_struct = communicate_models::MessageTextDTO {
            uuid: new_uuid.clone(),
            code: option_code::OptionCode::CommonOptionCode::ServerGetAndroidUsbFileByPiece as i32,
            content: content,
            content_type: 0
        };


        let future = async move {

            // let (tx, rx): (Sender<Bytes>, Receiver<Bytes>) = oneshot::channel();
            // sleep(Duration::from_secs(1)).await;

            debug!("after sel");
            Poll::Ready(None)
            // message_kernel_to_front_end(json_struct, tx);
            //
            // match rx.await {
            //     Ok(bytes) => {
            //         debug!("poll_next suc");
            //         Poll::Ready(Some(Ok(bytes)))
            //     }
            //     Err(err) => {
            //         debug!("poll_next future{:?}", err);
            //         Poll::Ready(None)
            //     }
            // }
        };


        debug!("poll_next pin future");
        let mut future = Box::pin(future);


        debug!("poll_next 后面代码");
        match future.as_mut().poll(cx) {
            Poll::Ready(result) => {
                debug!("poll_next return result");
                result
            },
            Poll::Pending => {
                debug!("poll_next return pending");
                Poll::Pending
            },
        }
    }
}
