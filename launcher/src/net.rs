//! HTTP helpers: JSON fetches and a small parallel, hash-verified downloader.
use crate::state::Shared;
use sha1::{Digest, Sha1};
use std::fs;
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use std::sync::atomic::Ordering;
use std::sync::{Arc, Mutex};
use std::time::Duration;

pub type Res<T> = Result<T, String>;

pub fn agent() -> ureq::Agent {
    ureq::AgentBuilder::new()
        .try_proxy_from_env(true)
        .timeout_connect(Duration::from_secs(20))
        .timeout_read(Duration::from_secs(60))
        .user_agent("LumenClient-Launcher/1.0")
        .build()
}

pub fn get_json(url: &str) -> Res<serde_json::Value> {
    let mut last = String::new();
    for attempt in 0..3 {
        match agent().get(url).call() {
            Ok(r) => return r.into_json().map_err(|e| format!("Bad JSON from {url}: {e}")),
            Err(e) => { last = e.to_string(); std::thread::sleep(Duration::from_millis(400 * (attempt + 1))); }
        }
    }
    Err(format!("Could not reach {url}: {last}"))
}

pub fn sha1_file(path: &Path) -> Option<String> {
    let mut f = fs::File::open(path).ok()?;
    let mut h = Sha1::new();
    let mut buf = [0u8; 64 * 1024];
    loop {
        let n = f.read(&mut buf).ok()?;
        if n == 0 { break; }
        h.update(&buf[..n]);
    }
    Some(h.finalize().iter().map(|b| format!("{b:02x}")).collect())
}

#[derive(Clone, Debug)]
pub struct Job {
    /// Tried in order; the first that works wins.
    pub urls: Vec<String>,
    pub dest: PathBuf,
    pub sha1: Option<String>,
    pub size: Option<u64>,
}

impl Job {
    pub fn new(url: impl Into<String>, dest: PathBuf, sha1: Option<String>, size: Option<u64>) -> Self {
        Job { urls: vec![url.into()], dest, sha1, size }
    }

    /// A file already on disk is trusted when its size matches (or it is simply non-empty when
    /// no size is known); the hash is checked at download time.
    fn present(&self) -> bool {
        match fs::metadata(&self.dest) {
            Ok(m) if m.is_file() => match self.size { Some(s) => m.len() == s, None => m.len() > 0 },
            _ => false,
        }
    }
}

pub fn download_one(job: &Job) -> Res<()> {
    if job.present() { return Ok(()); }
    if let Some(parent) = job.dest.parent() { fs::create_dir_all(parent).map_err(|e| e.to_string())?; }
    let tmp = job.dest.with_extension("part");
    let mut last = String::from("no URL");
    for url in &job.urls {
        for attempt in 0..3u64 {
            let result = (|| -> Res<()> {
                let resp = agent().get(url).call().map_err(|e| e.to_string())?;
                let mut reader = resp.into_reader();
                let mut out = fs::File::create(&tmp).map_err(|e| e.to_string())?;
                let mut buf = [0u8; 64 * 1024];
                loop {
                    let n = reader.read(&mut buf).map_err(|e| e.to_string())?;
                    if n == 0 { break; }
                    out.write_all(&buf[..n]).map_err(|e| e.to_string())?;
                }
                drop(out);
                if let Some(want) = &job.sha1 {
                    let got = sha1_file(&tmp).unwrap_or_default();
                    if &got != want { return Err(format!("checksum mismatch for {url}")); }
                }
                fs::rename(&tmp, &job.dest).map_err(|e| e.to_string())
            })();
            match result {
                Ok(()) => return Ok(()),
                Err(e) => {
                    last = e;
                    let _ = fs::remove_file(&tmp);
                    if last.contains("404") { break; }
                    std::thread::sleep(Duration::from_millis(300 * (attempt + 1)));
                }
            }
        }
    }
    Err(format!("Download failed: {} ({last})", job.dest.file_name().and_then(|n| n.to_str()).unwrap_or("?")))
}

/// Downloads every missing file with a pool of worker threads, updating the shared progress.
pub fn download_all(jobs: Vec<Job>, shared: &Arc<Shared>, label: &str) -> Res<()> {
    let jobs: Vec<Job> = jobs.into_iter().filter(|j| !j.present()).collect();
    if jobs.is_empty() { return Ok(()); }
    shared.set_status(&format!("{label} (0/{})", jobs.len()));
    shared.total.store(jobs.len(), Ordering::SeqCst);
    shared.done.store(0, Ordering::SeqCst);
    let total = jobs.len();
    let queue = Arc::new(Mutex::new(jobs));
    let error: Arc<Mutex<Option<String>>> = Arc::new(Mutex::new(None));
    let mut handles = Vec::new();
    for _ in 0..12usize.min(total) {
        let (queue, error, shared, label) = (queue.clone(), error.clone(), shared.clone(), label.to_string());
        handles.push(std::thread::spawn(move || loop {
            if error.lock().unwrap().is_some() { return; }
            let job = match queue.lock().unwrap().pop() { Some(j) => j, None => return };
            match download_one(&job) {
                Ok(()) => {
                    let d = shared.done.fetch_add(1, Ordering::SeqCst) + 1;
                    if d % 8 == 0 || d == total { shared.set_status(&format!("{label} ({d}/{total})")); }
                }
                Err(e) => { *error.lock().unwrap() = Some(e); return; }
            }
        }));
    }
    for h in handles { let _ = h.join(); }
    shared.total.store(0, Ordering::SeqCst);
    let failed = error.lock().unwrap().take();
    match failed { Some(e) => Err(e), None => Ok(()) }
}
