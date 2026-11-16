// Lumen Client launcher. The interface is drawn with egui; everything else is plain Rust.
#![cfg_attr(all(windows, not(debug_assertions)), windows_subsystem = "windows")]

mod auth;
mod content;
mod game;
mod launch;
mod net;
mod paths;
mod state;
mod ui;

use state::{Instance, Phase, Settings, Shared};
use std::sync::Arc;

/// Runs on a background thread: sign in, download what is missing, start the game.
pub fn play(inst: Instance, settings: Settings, shared: Arc<Shared>) {
    shared.set_phase(Phase::Working);
    let result = (|| -> net::Res<()> {
        let account = shared.account.lock().unwrap().clone().ok_or("Sign in with your Microsoft account first.")?;
        shared.set_status("Checking your account...");
        let account = auth::refresh(&settings.client_id, &account)?;
        *shared.account.lock().unwrap() = Some(account.clone());
        let profile = game::prepare(&inst, &shared)?;
        content::install(&inst, &shared)?;
        shared.set_status("Starting Minecraft...");
        launch::launch(&inst, &profile, &account, &settings, &shared)
    })();
    if let Err(e) = result {
        shared.log(format!("ERROR: {e}"));
        shared.set_phase(Phase::Failed(e));
    }
}

#[cfg(feature = "devtest")]
fn devtest(id: &str, secs: u64) -> ! {
    let inst = *state::INSTANCES.iter().find(|i| i.id == id).expect("unknown instance");
    let shared = Arc::new(Shared::new());
    *shared.account.lock().unwrap() = Some(state::Account { name: "Tester".into(), uuid: "00000000000000000000000000000000".into(), refresh_token: String::new(), access_token: "0".into() });
    let mut settings = Settings::default();
    settings.memory_mb = 1500;
    let s2 = shared.clone();
    std::thread::spawn(move || play(inst, settings, s2));
    let start = std::time::Instant::now();
    let mut last = String::new();
    let mut seen = 0usize;
    loop {
        std::thread::sleep(std::time::Duration::from_millis(500));
        let st = shared.status.lock().unwrap().clone();
        if st != last { println!("[status] {st}"); last = st; }
        let log = shared.log.lock().unwrap().clone();
        for l in log.iter().skip(seen.min(log.len())) { println!("[log] {l}"); }
        seen = log.len();
        match shared.phase() {
            Phase::Failed(e) => { println!("[devtest] FAILED: {e}"); std::process::exit(1); }
            Phase::Idle if start.elapsed().as_secs() > 5 => { println!("[devtest] game exited cleanly"); std::process::exit(0); }
            _ => {}
        }
        if start.elapsed().as_secs() > secs { println!("[devtest] still running after {secs}s -> OK"); std::process::exit(0); }
    }
}

fn main() {
    #[cfg(feature = "devtest")]
    {
        let args: Vec<String> = std::env::args().collect();
        if args.len() >= 3 && args[1] == "--devtest" {
            devtest(&args[2], args.get(3).and_then(|s| s.parse().ok()).unwrap_or(120));
        }
    }
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_title("Lumen Client")
            .with_inner_size([1040.0, 660.0])
            .with_min_inner_size([900.0, 580.0]),
        ..Default::default()
    };
    let _ = eframe::run_native("Lumen Client", options, Box::new(|cc| Ok(Box::new(ui::App::new(cc)))));
}
