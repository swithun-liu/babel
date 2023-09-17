use std::fs;
use log::debug;

pub fn list_children_files(path: &str) -> Vec<String> {
    let mut children: Vec<String> = Vec::new();

    match fs::read_dir(path) {
        Ok(entries) => {
            for entry in entries {
                match entry {
                    Ok(entry) => {
                        if let Some(name) = entry.file_name().to_str() {
                            let child_path = format!("{}/{}", path, name);
                            children.push(child_path);
                        }
                    }
                    Err(err) => {
                        debug!("Failed to read directory entry: {}", err);
                    }
                }
            }
        }
        Err(err) => {
            debug!("Failed to read directory: {}", err);
        }
    }

    children
}