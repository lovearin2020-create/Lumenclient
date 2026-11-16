//! Builds the Java command line and starts the game.
use crate::game::Profile;
use crate::net::Res;
use crate::paths;
use crate::state::{Account, Instance, Phase, Settings, Shared};
use std::fs;
use std::io::{BufRead, BufReader};
use std::process::{Command, Stdio};
use std::sync::Arc;

/// Legacy (1.8.9) LWJGL natives ship as jars and must be unpacked next to the game.
fn extract_natives(profile: &Profile, dir: &std::path::Path) -> Res<()> {
    fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    for lib in profile.libs.iter().filter(|l| l.extract) {
        let file = fs::File::open(&lib.path).map_err(|e| e.to_string())?;
        let mut zip = zip::ZipArchive::new(file).map_err(|e| e.to_string())?;
        for i in 0..zip.len() {
            let mut entry = zip.by_index(i).map_err(|e| e.to_string())?;
            let name = entry.name().to_string();
            if entry.is_dir() || name.starts_with("META-INF") || name.contains("..") { continue; }
            let out_path = dir.join(name.rsplit('/').next().unwrap_or(&name));
            if out_path.exists() { continue; }
            let mut out = fs::File::create(&out_path).map_err(|e| e.to_string())?;
            std::io::copy(&mut entry, &mut out).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}

pub fn launch(inst: &Instance, profile: &Profile, account: &Account, settings: &Settings, shared: &Arc<Shared>) -> Res<()> {
    let game_dir = paths::instance(inst.id);
    fs::create_dir_all(&game_dir).map_err(|e| e.to_string())?;
    let natives = game_dir.join("natives");
    extract_natives(profile, &natives)?;

    let sep = paths::classpath_separator();
    let mut cp: Vec<String> = profile.libs.iter().filter(|l| !l.extract).map(|l| l.path.to_string_lossy().to_string()).collect();
    cp.push(profile.client_jar.to_string_lossy().to_string());
    let classpath = cp.join(sep);

    let fill = |s: &str| -> String {
        s.replace("${auth_player_name}", &account.name)
            .replace("${version_name}", &profile.version_name)
            .replace("${game_directory}", &game_dir.to_string_lossy())
            .replace("${assets_root}", &paths::assets().to_string_lossy())
            .replace("${game_assets}", &paths::assets().to_string_lossy())
            .replace("${assets_index_name}", &profile.asset_index)
            .replace("${auth_uuid}", &account.uuid)
            .replace("${auth_access_token}", &account.access_token)
            .replace("${auth_session}", &account.access_token)
            .replace("${user_properties}", "{}")
            .replace("${user_type}", "msa")
            .replace("${version_type}", "Lumen")
            .replace("${clientid}", "")
            .replace("${auth_xuid}", "")
            .replace("${natives_directory}", &natives.to_string_lossy())
            .replace("${launcher_name}", "LumenClient")
            .replace("${launcher_version}", env!("CARGO_PKG_VERSION"))
            .replace("${library_directory}", &paths::libraries().to_string_lossy())
            .replace("${classpath_separator}", sep)
            .replace("${classpath}", &classpath)
    };

    let mut args: Vec<String> = vec![
        format!("-Xmx{}M", settings.memory_mb.clamp(1024, 16384)),
        "-Xms512M".into(),
        "-XX:+UnlockExperimentalVMOptions".into(), "-XX:+UseG1GC".into(), "-XX:G1NewSizePercent=20".into(),
        "-XX:G1ReservePercent=20".into(), "-XX:MaxGCPauseMillis=50".into(), "-XX:G1HeapRegionSize=32M".into(),
    ];
    if profile.legacy && cfg!(windows) {
        // Same switch the official launcher passes for old versions: Intel drivers pick their
        // high-performance path when they see this file name.
        args.push("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump".into());
    }
    args.extend(settings.extra_jvm_args.split_whitespace().map(String::from));
    args.extend(profile.jvm_args.iter().map(|a| fill(a)));
    args.push(profile.main_class.clone());
    args.extend(profile.game_args.iter().map(|a| fill(a)));

    let mut cmd = Command::new(&profile.java);
    cmd.args(&args).current_dir(&game_dir).stdin(Stdio::null()).stdout(Stdio::piped()).stderr(Stdio::piped());
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        cmd.creation_flags(0x0800_0000); // no console window
    }
    let mut child = cmd.spawn().map_err(|e| format!("Could not start Java ({}): {e}", profile.java.display()))?;
    shared.log(format!("Started Minecraft {} ({} libraries on the classpath)", inst.mc, cp.len()));
    shared.set_phase(Phase::Running);
    shared.set_status("Game is running");

    // Mirror the game's output into the launcher log; report how it exited.
    let (out, err) = (child.stdout.take(), child.stderr.take());
    let s2 = shared.clone();
    std::thread::spawn(move || { if let Some(e) = err { for line in BufReader::new(e).lines().map_while(Result::ok) { s2.log(line); } } });
    let s3 = shared.clone();
    std::thread::spawn(move || {
        if let Some(o) = out { for line in BufReader::new(o).lines().map_while(Result::ok) { s3.log(line); } }
        match child.wait() {
            Ok(st) if st.success() => { s3.set_phase(Phase::Idle); s3.set_status("Game closed"); }
            Ok(st) => {
                let code = st.code().map(|c| c.to_string()).unwrap_or_else(|| "unknown".into());
                s3.set_phase(Phase::Failed(format!("The game exited with code {code}. See the log below or the instance's crash-reports folder.")));
            }
            Err(e) => s3.set_phase(Phase::Failed(format!("Lost track of the game process: {e}"))),
        }
    });
    Ok(())
}
