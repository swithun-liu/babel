use rustorrent::{Client, TorrentId};
use std::path::PathBuf;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 创建一个BT客户端
    let client = Client::new("127.0.0.1:8080")?;

    // 添加一个种子文件并开始下载
    let path = PathBuf::from("/path/to/torrent/file.torrent");
    let torrent_id = client.add_torrent_file(path).await?;

    // 等待下载完成
    while !client.is_torrent_complete(&torrent_id).await? {
        tokio::time::delay_for(std::time::Duration::from_secs(1)).await;
    }

    Ok(())
}