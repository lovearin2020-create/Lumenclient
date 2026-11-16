//! Resolves and installs everything a version needs: the Minecraft client, its libraries and
//! assets, the mod loader (Forge for 1.8.9, Fabric for 26.x) and a matching Java runtime.
use crate::net::{self, Job, Res};
use crate::paths;
use crate::state::{Instance, Loader, Shared};
use serde_json::Value;
use std::fs;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::sync::Arc;

const MANIFEST: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
const JAVA_ALL: &str = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
const FORGE_VERSION: &str = "1.8.9-11.15.1.2318-1.8.9";
const FORGE_MAVEN: &str = "https://maven.minecraftforge.net/";
const MOJANG_LIBS: &str = "https://libraries.minecraft.net/";
const CENTRAL: &str = "https://repo1.maven.org/maven2/";

#[derive(Clone, Debug)]
pub struct Lib {
    /// "group:artifact", used so a loader's copy of a library replaces the vanilla one.
    pub key: String,
    pub path: PathBuf,
    /// Legacy (1.8.9) native jars are unpacked instead of being put on the classpath.
    pub extract: bool,
}

/// Everything needed to start one instance.
#[derive(Clone, Debug)]
pub struct Profile {
    pub version_name: String,
    pub main_class: String,
    pub libs: Vec<Lib>,
    pub client_jar: PathBuf,
    pub asset_index: String,
    pub game_args: Vec<String>,
    pub jvm_args: Vec<String>,
    pub java: PathBuf,
    pub legacy: bool,
}

/// "group:artifact:version[:classifier][@ext]" -> relative Maven path.
pub fn maven_path(name: &str) -> String {
    let (coords, ext) = match name.split_once('@') { Some((c, e)) => (c, e), None => (name, "jar") };
    let p: Vec<&str> = coords.split(':').collect();
    let (group, artifact, version) = (p[0], p[1], p[2]);
    let classifier = p.get(3).map(|c| format!("-{c}")).unwrap_or_default();
    format!("{}/{artifact}/{version}/{artifact}-{version}{classifier}.{ext}", group.replace('.', "/"))
}

fn lib_key(name: &str) -> String {
    let p: Vec<&str> = name.split('@').next().unwrap_or(name).split(':').collect();
    match p.get(3) { Some(c) => format!("{}:{}:{c}", p[0], p[1]), None => format!("{}:{}", p[0], p[1]) }
}

/// Mojang "rules": the last matching rule decides; no rules means allowed.
fn rules_allow(rules: Option<&Value>) -> bool {
    let rules = match rules.and_then(|r| r.as_array()) { Some(r) => r, None => return true };
    let mut allowed = false;
    for rule in rules {
        if rule.get("features").is_some() { return false; } // demo mode, custom resolution, quick play...
        let mut matches = true;
        if let Some(os) = rule.get("os") {
            if let Some(name) = os.get("name").and_then(|n| n.as_str()) { matches &= name == paths::os_name(); }
            if let Some(arch) = os.get("arch").and_then(|a| a.as_str()) {
                matches &= match arch { "x86" => cfg!(target_arch = "x86"), "arm64" => cfg!(target_arch = "aarch64"), _ => true };
            }
        }
        if matches { allowed = rule.get("action").and_then(|a| a.as_str()) == Some("allow"); }
    }
    allowed
}

fn read_json(path: &Path) -> Option<Value> { serde_json::from_str(&fs::read_to_string(path).ok()?).ok() }

fn cached_json(url: &str, path: &Path) -> Res<Value> {
    if let Some(v) = read_json(path) { return Ok(v); }
    let v = net::get_json(url)?;
    if let Some(p) = path.parent() { fs::create_dir_all(p).map_err(|e| e.to_string())?; }
    fs::write(path, serde_json::to_string(&v).unwrap()).map_err(|e| e.to_string())?;
    Ok(v)
}

struct Vanilla {
    json: Value,
    jobs: Vec<Job>,
    libs: Vec<Lib>,
    client_jar: PathBuf,
}

fn vanilla(mc: &str) -> Res<Vanilla> {
    let dir = paths::versions().join(mc);
    let json_path = dir.join(format!("{mc}.json"));
    let json = match read_json(&json_path) {
        Some(j) => j,
        None => {
            let manifest = net::get_json(MANIFEST)?;
            let url = manifest["versions"].as_array().and_then(|v| v.iter().find(|x| x["id"] == mc))
                .and_then(|x| x["url"].as_str()).ok_or(format!("Minecraft {mc} was not found in Mojang's version list"))?.to_string();
            cached_json(&url, &json_path)?
        }
    };
    let mut jobs = Vec::new();
    let mut libs = Vec::new();
    let client_jar = dir.join(format!("{mc}.jar"));
    let c = &json["downloads"]["client"];
    jobs.push(Job::new(c["url"].as_str().unwrap_or_default(), client_jar.clone(), c["sha1"].as_str().map(String::from), c["size"].as_u64()));

    for lib in json["libraries"].as_array().cloned().unwrap_or_default() {
        if !rules_allow(lib.get("rules")) { continue; }
        let name = lib["name"].as_str().unwrap_or_default().to_string();
        let mut add = |art: &Value, extract: bool, key: String| {
            if let (Some(path), Some(url)) = (art["path"].as_str(), art["url"].as_str()) {
                let dest = paths::libraries().join(path);
                jobs.push(Job::new(url, dest.clone(), art["sha1"].as_str().map(String::from), art["size"].as_u64()));
                libs.push(Lib { key, path: dest, extract });
            }
        };
        // Legacy natives: a classifier jar per OS that must be unpacked.
        if let Some(classifier) = lib["natives"][paths::os_name()].as_str() {
            let classifier = classifier.replace("${arch}", "64");
            let art = lib["downloads"]["classifiers"][&classifier].clone();
            add(&art, true, format!("{}:{classifier}", lib_key(&name)));
        }
        if lib["downloads"]["artifact"].is_object() {
            add(&lib["downloads"]["artifact"], false, lib_key(&name));
        }
    }
    Ok(Vanilla { json, jobs, libs, client_jar })
}

fn asset_jobs(json: &Value) -> Res<(String, Vec<Job>)> {
    let idx = &json["assetIndex"];
    let id = idx["id"].as_str().unwrap_or("legacy").to_string();
    let path = paths::assets().join("indexes").join(format!("{id}.json"));
    let index = cached_json(idx["url"].as_str().unwrap_or_default(), &path)?;
    let mut jobs = Vec::new();
    if let Some(objects) = index["objects"].as_object() {
        for obj in objects.values() {
            let hash = obj["hash"].as_str().unwrap_or_default();
            if hash.len() < 2 { continue; }
            let rel = format!("{}/{hash}", &hash[..2]);
            jobs.push(Job::new(format!("https://resources.download.minecraft.net/{rel}"), paths::assets().join("objects").join(&rel), Some(hash.to_string()), obj["size"].as_u64()));
        }
    }
    Ok((id, jobs))
}

/// Mojang's own Java builds: Java 8 for 1.8.9, the newer runtime for 26.x.
fn java_runtime(json: &Value, shared: &Arc<Shared>) -> Res<PathBuf> {
    let component = json["javaVersion"]["component"].as_str().unwrap_or("jre-legacy").to_string();
    let os_key = if cfg!(windows) {
        if cfg!(target_arch = "aarch64") { "windows-arm64" } else { "windows-x64" }
    } else if cfg!(target_os = "macos") {
        if cfg!(target_arch = "aarch64") { "mac-os-arm64" } else { "mac-os" }
    } else { "linux" };
    let dir = paths::runtimes().join(&component);
    let exe = if cfg!(windows) { dir.join("bin").join("javaw.exe") } else { dir.join("bin").join("java") };
    let marker = dir.join(".complete");
    if marker.exists() && exe.exists() { return Ok(exe); }

    shared.set_status("Preparing Java...");
    let all = net::get_json(JAVA_ALL)?;
    let manifest_url = all[os_key][&component][0]["manifest"]["url"].as_str()
        .ok_or(format!("Mojang has no '{component}' Java runtime for {os_key}"))?.to_string();
    let manifest = net::get_json(&manifest_url)?;
    let mut jobs = Vec::new();
    let mut executables = Vec::new();
    for (rel, entry) in manifest["files"].as_object().cloned().unwrap_or_default() {
        let dest = dir.join(&rel);
        match entry["type"].as_str() {
            Some("directory") => { fs::create_dir_all(&dest).map_err(|e| e.to_string())?; }
            Some("file") => {
                let raw = &entry["downloads"]["raw"];
                jobs.push(Job::new(raw["url"].as_str().unwrap_or_default(), dest.clone(), raw["sha1"].as_str().map(String::from), raw["size"].as_u64()));
                if entry["executable"].as_bool() == Some(true) { executables.push(dest); }
            }
            _ => {}
        }
    }
    net::download_all(jobs, shared, "Downloading Java")?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        for e in &executables { let _ = fs::set_permissions(e, fs::Permissions::from_mode(0o755)); }
    }
    let _ = executables;
    fs::write(&marker, component).map_err(|e| e.to_string())?;
    Ok(exe)
}

fn legacy_game_args(s: &str) -> Vec<String> { s.split_whitespace().map(String::from).collect() }

fn modern_args(list: &Value) -> Vec<String> {
    let mut out = Vec::new();
    for a in list.as_array().cloned().unwrap_or_default() {
        if let Some(s) = a.as_str() { out.push(s.to_string()); continue; }
        if !rules_allow(a.get("rules")) { continue; }
        match &a["value"] {
            Value::String(s) => out.push(s.clone()),
            Value::Array(v) => out.extend(v.iter().filter_map(|x| x.as_str().map(String::from))),
            _ => {}
        }
    }
    out
}

/// Forge 1.8.9: the installer jar carries the profile and the universal jar.
fn forge(shared: &Arc<Shared>) -> Res<(String, Vec<String>, Vec<Lib>, Vec<Job>)> {
    let dir = paths::versions().join(format!("forge-{FORGE_VERSION}"));
    let installer = dir.join("installer.jar");
    net::download_one(&Job::new(
        format!("{FORGE_MAVEN}net/minecraftforge/forge/{FORGE_VERSION}/forge-{FORGE_VERSION}-installer.jar"), installer.clone(), None, None))?;
    shared.set_status("Reading Forge profile...");
    let file = fs::File::open(&installer).map_err(|e| e.to_string())?;
    let mut zip = zip::ZipArchive::new(file).map_err(|e| format!("Forge installer is corrupt: {e}"))?;
    let mut text = String::new();
    zip.by_name("install_profile.json").map_err(|e| e.to_string())?.read_to_string(&mut text).map_err(|e| e.to_string())?;
    let profile: Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
    let info = &profile["versionInfo"];

    // The Forge jar itself ships inside the installer.
    let forge_name = profile["install"]["path"].as_str().unwrap_or_default().to_string();
    let forge_file = profile["install"]["filePath"].as_str().unwrap_or_default().to_string();
    let forge_dest = paths::libraries().join(maven_path(&forge_name));
    if !forge_dest.exists() {
        fs::create_dir_all(forge_dest.parent().unwrap()).map_err(|e| e.to_string())?;
        let mut entry = zip.by_name(&forge_file).map_err(|e| e.to_string())?;
        let mut out = fs::File::create(&forge_dest).map_err(|e| e.to_string())?;
        std::io::copy(&mut entry, &mut out).map_err(|e| e.to_string())?;
    }

    let mut libs = vec![Lib { key: lib_key(&forge_name), path: forge_dest, extract: false }];
    let mut jobs = Vec::new();
    for lib in info["libraries"].as_array().cloned().unwrap_or_default() {
        let name = lib["name"].as_str().unwrap_or_default().to_string();
        if name == forge_name || lib["clientreq"].as_bool() == Some(false) { continue; }
        let rel = maven_path(&name);
        let dest = paths::libraries().join(&rel);
        // Forge's old profile points at a retired host; try the current mirrors in turn.
        let urls = vec![format!("{FORGE_MAVEN}{rel}"), format!("{MOJANG_LIBS}{rel}"), format!("{CENTRAL}{rel}")];
        jobs.push(Job { urls, dest: dest.clone(), sha1: None, size: None });
        libs.push(Lib { key: lib_key(&name), path: dest, extract: false });
    }
    let main = info["mainClass"].as_str().unwrap_or("net.minecraft.launchwrapper.Launch").to_string();
    let args = legacy_game_args(info["minecraftArguments"].as_str().unwrap_or_default());
    Ok((main, args, libs, jobs))
}

/// Fabric publishes a ready-made launcher profile per game version.
fn fabric(mc: &str) -> Res<(String, Vec<String>, Vec<String>, Vec<Lib>, Vec<Job>)> {
    let dir = paths::versions().join(format!("fabric-{mc}"));
    let path = dir.join("profile.json");
    let profile = match read_json(&path) {
        Some(p) => p,
        None => {
            let loaders = net::get_json(&format!("https://meta.fabricmc.net/v2/versions/loader/{mc}"))?;
            let list = loaders.as_array().cloned().unwrap_or_default();
            let pick = list.iter().find(|l| l["loader"]["stable"].as_bool() == Some(true)).or(list.first())
                .ok_or(format!("Fabric does not support Minecraft {mc} yet"))?;
            let loader = pick["loader"]["version"].as_str().unwrap_or_default();
            cached_json(&format!("https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json"), &path)?
        }
    };
    let mut libs = Vec::new();
    let mut jobs = Vec::new();
    for lib in profile["libraries"].as_array().cloned().unwrap_or_default() {
        let name = lib["name"].as_str().unwrap_or_default().to_string();
        let rel = maven_path(&name);
        let base = lib["url"].as_str().unwrap_or("https://maven.fabricmc.net/");
        let dest = paths::libraries().join(&rel);
        jobs.push(Job::new(format!("{base}{rel}"), dest.clone(), lib["sha1"].as_str().map(String::from), lib["size"].as_u64()));
        libs.push(Lib { key: lib_key(&name), path: dest, extract: false });
    }
    let main = profile["mainClass"].as_str().unwrap_or("net.fabricmc.loader.impl.launch.knot.KnotClient").to_string();
    Ok((main, modern_args(&profile["arguments"]["game"]), modern_args(&profile["arguments"]["jvm"]), libs, jobs))
}

/// Downloads whatever is missing for the instance and returns how to start it.
pub fn prepare(inst: &Instance, shared: &Arc<Shared>) -> Res<Profile> {
    shared.set_status(&format!("Checking Minecraft {}...", inst.mc));
    let v = vanilla(inst.mc)?;
    let legacy = v.json.get("minecraftArguments").is_some();

    let mut jobs = v.jobs.clone();
    let mut libs: Vec<Lib> = Vec::new();
    let main_class;
    let mut game_args = if legacy { legacy_game_args(v.json["minecraftArguments"].as_str().unwrap_or_default()) } else { modern_args(&v.json["arguments"]["game"]) };
    let mut jvm_args = if legacy {
        vec!["-Djava.library.path=${natives_directory}".to_string(), "-cp".to_string(), "${classpath}".to_string()]
    } else { modern_args(&v.json["arguments"]["jvm"]) };

    match inst.loader {
        Loader::Forge => {
            let (main, args, flibs, fjobs) = forge(shared)?;
            main_class = main;
            game_args = args;
            libs.extend(flibs);
            jobs.extend(fjobs);
        }
        Loader::Fabric => {
            let (main, gargs, jargs, flibs, fjobs) = fabric(inst.mc)?;
            main_class = main;
            game_args.extend(gargs);
            // Loader JVM flags go before the vanilla ones (which end with -cp ... mainClass order).
            let mut merged = jargs;
            merged.extend(jvm_args);
            jvm_args = merged;
            libs.extend(flibs);
            jobs.extend(fjobs);
        }
    }
    for lib in v.libs {
        if !libs.iter().any(|l| l.key == lib.key) { libs.push(lib); }
    }

    net::download_all(jobs, shared, "Downloading game files")?;
    let (asset_index, assets) = asset_jobs(&v.json)?;
    net::download_all(assets, shared, "Downloading assets")?;
    let java = java_runtime(&v.json, shared)?;

    Ok(Profile {
        version_name: format!("{}-{}", if inst.loader == Loader::Forge { "forge" } else { "fabric" }, inst.mc),
        main_class, libs, client_jar: v.client_jar, asset_index, game_args, jvm_args, java, legacy,
    })
}
