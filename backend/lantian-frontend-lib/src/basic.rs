use android_logger::Config;
use log::Level;

pub fn init_debugger() {
    let config = Config::default().with_min_level(Level::Debug);
    android_logger::init_once(config);
}