//! Where everything lives on disk. All launcher data sits under one folder so it can be
//! backed up or moved as a unit:
//!
//!   <data>/LumenClient/
//!     settings.json, account.json
//!     versions/   libraries/   assets/   runtimes/      (shared game files)
//!     instances/<id>/                                   (one game folder per version)
//!         mods/  config/  resourcepacks/  options.txt ...
use std::path::PathBuf;

pub fn root() -> PathBuf {
    let base = dirs::data_dir().unwrap_or_else(|| PathBuf::from("."));
    base.join("LumenClient")
}
pub fn versions() -> PathBuf { root().join("versions") }
pub fn libraries() -> PathBuf { root().join("libraries") }
pub fn assets() -> PathBuf { root().join("assets") }
pub fn runtimes() -> PathBuf { root().join("runtimes") }
pub fn instance(id: &str) -> PathBuf { root().join("instances").join(id) }

/// The official launcher's game folder, used only to import existing settings and OptiFine.
pub fn official_minecraft() -> PathBuf {
    if cfg!(windows) {
        dirs::data_dir().unwrap_or_default().join(".minecraft")
    } else if cfg!(target_os = "macos") {
        dirs::data_dir().unwrap_or_default().join("minecraft")
    } else {
        dirs::home_dir().unwrap_or_default().join(".minecraft")
    }
}

pub fn os_name() -> &'static str {
    if cfg!(windows) { "windows" } else if cfg!(target_os = "macos") { "osx" } else { "linux" }
}

pub fn classpath_separator() -> &'static str { if cfg!(windows) { ";" } else { ":" } }
