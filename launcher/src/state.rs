//! State shared between the UI thread and the background worker.
use serde::{Deserialize, Serialize};
use std::sync::atomic::AtomicUsize;
use std::sync::Mutex;

#[derive(Clone, PartialEq, Debug)]
pub enum Phase {
    Idle,
    Working,
    Running,
    Failed(String),
}

pub struct Shared {
    pub phase: Mutex<Phase>,
    pub status: Mutex<String>,
    pub done: AtomicUsize,
    pub total: AtomicUsize,
    pub log: Mutex<Vec<String>>,
    /// Microsoft sign-in prompt: (code to type, page to open).
    pub login_prompt: Mutex<Option<(String, String)>>,
    pub account: Mutex<Option<Account>>,
}

impl Shared {
    pub fn new() -> Self {
        Shared {
            phase: Mutex::new(Phase::Idle),
            status: Mutex::new(String::new()),
            done: AtomicUsize::new(0),
            total: AtomicUsize::new(0),
            log: Mutex::new(Vec::new()),
            login_prompt: Mutex::new(None),
            account: Mutex::new(None),
        }
    }
    pub fn set_status(&self, s: &str) { *self.status.lock().unwrap() = s.to_string(); }
    pub fn set_phase(&self, p: Phase) { *self.phase.lock().unwrap() = p; }
    pub fn phase(&self) -> Phase { self.phase.lock().unwrap().clone() }
    pub fn log(&self, s: impl Into<String>) {
        let mut l = self.log.lock().unwrap();
        l.push(s.into());
        if l.len() > 400 { let cut = l.len() - 400; l.drain(..cut); }
    }
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct Account {
    pub name: String,
    pub uuid: String,
    pub refresh_token: String,
    #[serde(skip)]
    pub access_token: String,
}

#[derive(Clone, Serialize, Deserialize)]
#[serde(default)]
pub struct Settings {
    /// Azure application (client) ID used for Microsoft sign-in.
    pub client_id: String,
    pub memory_mb: u32,
    pub extra_jvm_args: String,
    pub close_on_launch: bool,
    pub last_instance: String,
}

impl Default for Settings {
    fn default() -> Self {
        Settings { client_id: String::new(), memory_mb: 3072, extra_jvm_args: String::new(), close_on_launch: false, last_instance: "forge-1.8.9".into() }
    }
}

impl Settings {
    fn path() -> std::path::PathBuf { crate::paths::root().join("settings.json") }
    pub fn load() -> Self {
        std::fs::read_to_string(Self::path()).ok().and_then(|s| serde_json::from_str(&s).ok()).unwrap_or_default()
    }
    pub fn save(&self) {
        let _ = std::fs::create_dir_all(crate::paths::root());
        if let Ok(s) = serde_json::to_string_pretty(self) { let _ = std::fs::write(Self::path(), s); }
    }
}

#[derive(Clone, Copy, PartialEq, Debug)]
pub enum Loader { Forge, Fabric }

#[derive(Clone, Copy, PartialEq, Debug)]
pub struct Instance {
    pub id: &'static str,
    pub title: &'static str,
    pub subtitle: &'static str,
    pub mc: &'static str,
    pub loader: Loader,
}

pub const INSTANCES: [Instance; 3] = [
    Instance { id: "forge-1.8.9", title: "1.8.9", subtitle: "Forge  |  Lumen + OptiFine", mc: "1.8.9", loader: Loader::Forge },
    Instance { id: "fabric-26.3", title: "26.3", subtitle: "Fabric  |  Sodium", mc: "26.3", loader: Loader::Fabric },
    Instance { id: "fabric-26.2", title: "26.2", subtitle: "Fabric  |  Sodium", mc: "26.2", loader: Loader::Fabric },
];
