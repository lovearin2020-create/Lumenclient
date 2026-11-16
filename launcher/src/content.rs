//! Per-instance content: the bundled Lumen mod, OptiFine import, Sodium download,
//! the mod list, settings import and Lumen settings profiles.
use crate::net::{self, Job, Res};
use crate::paths;
use crate::state::{Instance, Loader, Shared};
use std::fs;
use std::path::{Path, PathBuf};
use std::sync::Arc;

/// The Lumen mod is compiled into the launcher and written into the 1.8.9 instance.
const LUMEN_JAR: &[u8] = include_bytes!("../assets/LumenClient.jar");
pub const LUMEN_VERSION: &str = "2.1.0";

pub struct ModEntry {
    pub file: String,
    pub enabled: bool,
    pub size: u64,
}

fn mods_dir(inst: &Instance) -> PathBuf { paths::instance(inst.id).join("mods") }

fn has_mod(inst: &Instance, needle: &str) -> bool {
    list_mods(inst).iter().any(|m| m.file.to_lowercase().contains(needle))
}

pub fn list_mods(inst: &Instance) -> Vec<ModEntry> {
    let mut out = Vec::new();
    if let Ok(rd) = fs::read_dir(mods_dir(inst)) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            let enabled = name.ends_with(".jar");
            if !enabled && !name.ends_with(".jar.disabled") { continue; }
            out.push(ModEntry { file: name, enabled, size: e.metadata().map(|m| m.len()).unwrap_or(0) });
        }
    }
    out.sort_by(|a, b| a.file.to_lowercase().cmp(&b.file.to_lowercase()));
    out
}

/// Enabling/disabling a mod just renames it between ".jar" and ".jar.disabled".
pub fn toggle_mod(inst: &Instance, file: &str) -> Res<()> {
    let dir = mods_dir(inst);
    let to = match file.strip_suffix(".disabled") { Some(base) => base.to_string(), None => format!("{file}.disabled") };
    fs::rename(dir.join(file), dir.join(to)).map_err(|e| e.to_string())
}

/// Looks for an OptiFine 1.8.9 jar the user already has (official mods folder, then Downloads).
pub fn find_optifine() -> Option<PathBuf> {
    let mut dirs_to_check = vec![paths::official_minecraft().join("mods")];
    if let Some(d) = dirs::download_dir() { dirs_to_check.push(d); }
    for dir in dirs_to_check {
        let mut hits: Vec<PathBuf> = fs::read_dir(&dir).ok()?.flatten().map(|e| e.path()).filter(|p| {
            let n = p.file_name().and_then(|n| n.to_str()).unwrap_or("").to_lowercase();
            n.contains("optifine") && n.contains("1.8.9") && n.ends_with(".jar")
        }).collect();
        hits.sort();
        if let Some(p) = hits.pop() { return Some(p); }
    }
    None
}

pub fn import_optifine(inst: &Instance, from: &Path) -> Res<String> {
    let name = from.file_name().and_then(|n| n.to_str()).ok_or("That is not a file")?.to_string();
    if !name.to_lowercase().ends_with(".jar") { return Err("OptiFine must be a .jar file".into()); }
    if !from.is_file() { return Err(format!("File not found: {}", from.display())); }
    fs::create_dir_all(mods_dir(inst)).map_err(|e| e.to_string())?;
    fs::copy(from, mods_dir(inst).join(&name)).map_err(|e| e.to_string())?;
    Ok(name)
}

/// Puts the default mods in place. Never overwrites or removes mods the user added.
pub fn install(inst: &Instance, shared: &Arc<Shared>) -> Res<()> {
    let dir = mods_dir(inst);
    fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    fs::create_dir_all(paths::instance(inst.id).join("config")).map_err(|e| e.to_string())?;
    match inst.loader {
        Loader::Forge => {
            let wanted = format!("LumenClient-{LUMEN_VERSION}-1.8.9.jar");
            let mut disabled = false;
            for m in list_mods(inst) {
                if m.file.starts_with("LumenClient-") && !m.file.starts_with(&wanted) {
                    disabled |= !m.enabled;
                    let _ = fs::remove_file(dir.join(&m.file));
                } else if m.file.starts_with(&wanted) && !m.enabled { disabled = true; }
            }
            let target = dir.join(if disabled { format!("{wanted}.disabled") } else { wanted });
            if fs::metadata(&target).map(|m| m.len() != LUMEN_JAR.len() as u64).unwrap_or(true) {
                fs::write(&target, LUMEN_JAR).map_err(|e| e.to_string())?;
            }
            if !has_mod(inst, "optifine") {
                match find_optifine() {
                    Some(p) => { let n = import_optifine(inst, &p)?; shared.log(format!("Imported {n} from {}", p.display())); }
                    None => shared.log("OptiFine was not found on this PC. Add it from the Mods tab to use it."),
                }
            }
        }
        Loader::Fabric => {
            if !has_mod(inst, "sodium") {
                shared.set_status("Getting Sodium...");
                let url = format!("https://api.modrinth.com/v2/project/sodium/version?game_versions=%5B%22{}%22%5D&loaders=%5B%22fabric%22%5D", inst.mc);
                let versions = net::get_json(&url)?;
                let list = versions.as_array().cloned().unwrap_or_default();
                let pick = list.iter().find(|v| v["version_type"] == "release").or(list.first());
                match pick {
                    Some(v) => {
                        let f = &v["files"][0];
                        let name = f["filename"].as_str().unwrap_or("sodium.jar").to_string();
                        net::download_one(&Job::new(f["url"].as_str().unwrap_or_default(), dir.join(&name), f["hashes"]["sha1"].as_str().map(String::from), f["size"].as_u64()))?;
                        shared.log(format!("Installed {name} from Modrinth"));
                    }
                    None => shared.log(format!("Sodium has no build for Minecraft {} yet; starting without it.", inst.mc)),
                }
            }
        }
    }
    Ok(())
}

fn copy_dir(from: &Path, to: &Path, count: &mut usize) -> std::io::Result<()> {
    fs::create_dir_all(to)?;
    for e in fs::read_dir(from)?.flatten() {
        let (src, dst) = (e.path(), to.join(e.file_name()));
        if src.is_dir() { copy_dir(&src, &dst, count)?; } else { fs::copy(&src, &dst)?; *count += 1; }
    }
    Ok(())
}

/// Copies controls, video settings, the server list and mod configs from the official
/// launcher's .minecraft into this instance.
pub fn import_settings(inst: &Instance) -> Res<usize> {
    let from = paths::official_minecraft();
    if !from.is_dir() { return Err(format!("No official game folder at {}", from.display())); }
    let to = paths::instance(inst.id);
    fs::create_dir_all(&to).map_err(|e| e.to_string())?;
    let mut n = 0;
    for f in ["options.txt", "optionsof.txt", "servers.dat"] {
        if from.join(f).is_file() && fs::copy(from.join(f), to.join(f)).is_ok() { n += 1; }
    }
    if inst.loader == Loader::Forge {
        for d in ["config", "OneConfig", "sk1ermod"] {
            if from.join(d).is_dir() { copy_dir(&from.join(d), &to.join(d), &mut n).map_err(|e| e.to_string())?; }
        }
    }
    Ok(n)
}

// ---- Lumen settings profiles (config/lumen/profiles/NAME.json inside the 1.8.9 instance)

fn lumen_dir(inst: &Instance) -> PathBuf { paths::instance(inst.id).join("config").join("lumen") }

pub fn active_profile(inst: &Instance) -> String {
    fs::read_to_string(lumen_dir(inst).join("config.json")).ok()
        .and_then(|s| serde_json::from_str::<serde_json::Value>(&s).ok())
        .and_then(|v| v["_client"]["profile"].as_str().map(String::from)).unwrap_or_default()
}

pub fn list_profiles(inst: &Instance) -> Vec<String> {
    let mut out: Vec<String> = fs::read_dir(lumen_dir(inst).join("profiles")).map(|rd| rd.flatten()
        .filter_map(|e| e.file_name().to_str().and_then(|n| n.strip_suffix(".json")).map(String::from)).collect()).unwrap_or_default();
    out.sort_by_key(|a| a.to_lowercase());
    out
}

/// Makes a profile the live configuration: the game reads config.json on its next start.
pub fn activate_profile(inst: &Instance, name: &str) -> Res<()> {
    let src = lumen_dir(inst).join("profiles").join(format!("{name}.json"));
    let text = fs::read_to_string(&src).map_err(|e| e.to_string())?;
    let mut v: serde_json::Value = serde_json::from_str(&text).map_err(|e| format!("Profile is not valid JSON: {e}"))?;
    v["_client"]["profile"] = serde_json::Value::String(name.to_string());
    fs::write(lumen_dir(inst).join("config.json"), serde_json::to_string_pretty(&v).unwrap()).map_err(|e| e.to_string())
}

pub fn delete_profile(inst: &Instance, name: &str) -> Res<()> {
    fs::remove_file(lumen_dir(inst).join("profiles").join(format!("{name}.json"))).map_err(|e| e.to_string())
}

/// Saves the instance's current settings as a new named profile.
pub fn save_current_as(inst: &Instance, name: &str) -> Res<()> {
    let clean: String = name.chars().filter(|c| c.is_ascii_alphanumeric() || *c == ' ' || *c == '_' || *c == '-').take(24).collect();
    let clean = clean.trim().to_string();
    if clean.is_empty() { return Err("Enter a profile name (letters, numbers, spaces, - or _)".into()); }
    let cfg = lumen_dir(inst).join("config.json");
    if !cfg.is_file() { return Err("No settings yet. Start the game once first.".into()); }
    fs::create_dir_all(lumen_dir(inst).join("profiles")).map_err(|e| e.to_string())?;
    fs::copy(cfg, lumen_dir(inst).join("profiles").join(format!("{clean}.json"))).map_err(|e| e.to_string())?;
    Ok(())
}
