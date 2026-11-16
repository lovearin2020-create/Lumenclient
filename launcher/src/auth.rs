//! Microsoft account sign-in (OAuth device-code flow) -> Xbox Live -> Minecraft services.
//!
//! This needs an Azure application (client) ID that Mojang has approved for the Minecraft API;
//! the launcher has none built in, the user enters their own in Settings.
use crate::net::{agent, Res};
use crate::paths;
use crate::state::{Account, Shared};
use serde_json::{json, Value};
use std::sync::Arc;
use std::time::{Duration, Instant};

const DEVICE_URL: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
const TOKEN_URL: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
const SCOPE: &str = "XboxLive.signin offline_access";

fn body(r: Result<ureq::Response, ureq::Error>) -> Result<Value, (u16, Value)> {
    match r {
        Ok(resp) => resp.into_json().map_err(|e| (0, json!({ "error": e.to_string() }))),
        Err(ureq::Error::Status(code, resp)) => Err((code, resp.into_json().unwrap_or(Value::Null))),
        Err(e) => Err((0, json!({ "error": e.to_string() }))),
    }
}

fn describe(what: &str, err: (u16, Value)) -> String {
    let (code, v) = err;
    let detail = v["error_description"].as_str().or(v["errorMessage"].as_str()).or(v["error"].as_str()).unwrap_or("no details").to_string();
    format!("{what} failed (HTTP {code}): {detail}")
}

fn account_path() -> std::path::PathBuf { paths::root().join("account.json") }

pub fn load_account() -> Option<Account> {
    serde_json::from_str(&std::fs::read_to_string(account_path()).ok()?).ok()
}

fn save_account(a: &Account) {
    let _ = std::fs::create_dir_all(paths::root());
    if let Ok(s) = serde_json::to_string_pretty(a) { let _ = std::fs::write(account_path(), s); }
}

pub fn sign_out() { let _ = std::fs::remove_file(account_path()); }

/// Xbox Live -> XSTS -> Minecraft token -> profile.
fn minecraft_login(ms_access: &str, refresh_token: String) -> Res<Account> {
    let xbl = body(agent().post("https://user.auth.xboxlive.com/user/authenticate").set("Accept", "application/json").send_json(json!({
        "Properties": { "AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com", "RpsTicket": format!("d={ms_access}") },
        "RelyingParty": "http://auth.xboxlive.com", "TokenType": "JWT"
    }))).map_err(|e| describe("Xbox Live sign-in", e))?;
    let xbl_token = xbl["Token"].as_str().ok_or("Xbox Live returned no token")?;
    let uhs = xbl["DisplayClaims"]["xui"][0]["uhs"].as_str().ok_or("Xbox Live returned no user hash")?.to_string();

    let xsts = body(agent().post("https://xsts.auth.xboxlive.com/xsts/authorize").set("Accept", "application/json").send_json(json!({
        "Properties": { "SandboxId": "RETAIL", "UserTokens": [xbl_token] },
        "RelyingParty": "rp://api.minecraftservices.com/", "TokenType": "JWT"
    }))).map_err(|(code, v)| match v["XErr"].as_u64() {
        Some(2148916233) => "This Microsoft account has no Xbox profile yet. Sign in at xbox.com once, then try again.".to_string(),
        Some(2148916235) => "Xbox Live is not available in this account's region.".to_string(),
        Some(2148916236) | Some(2148916237) => "This account needs adult verification on the Xbox website.".to_string(),
        Some(2148916238) => "This is a child account and must be added to a Microsoft family by an adult.".to_string(),
        _ => describe("Xbox authorization", (code, v)),
    })?;
    let xsts_token = xsts["Token"].as_str().ok_or("Xbox returned no XSTS token")?;

    let mc = body(agent().post("https://api.minecraftservices.com/authentication/login_with_xbox").send_json(json!({
        "identityToken": format!("XBL3.0 x={uhs};{xsts_token}")
    }))).map_err(|(code, v)| if code == 403 {
        "Minecraft rejected this app ID (HTTP 403). The Azure app must be approved by Mojang for the Minecraft API before it can sign in.".to_string()
    } else { describe("Minecraft sign-in", (code, v)) })?;
    let access = mc["access_token"].as_str().ok_or("Minecraft returned no access token")?.to_string();

    let profile = body(agent().get("https://api.minecraftservices.com/minecraft/profile").set("Authorization", &format!("Bearer {access}")).call())
        .map_err(|(code, v)| if code == 404 { "This account does not own Minecraft: Java Edition.".to_string() } else { describe("Loading the Minecraft profile", (code, v)) })?;
    let account = Account {
        name: profile["name"].as_str().ok_or("Profile has no name")?.to_string(),
        uuid: profile["id"].as_str().ok_or("Profile has no id")?.to_string(),
        refresh_token,
        access_token: access,
    };
    save_account(&account);
    Ok(account)
}

/// Starts a sign-in: shows a code for the user to enter on Microsoft's page, then waits for it.
pub fn sign_in(client_id: &str, shared: &Arc<Shared>) -> Res<Account> {
    if client_id.trim().is_empty() { return Err("Enter your Azure application (client) ID in Settings first.".into()); }
    let dc = body(agent().post(DEVICE_URL).send_form(&[("client_id", client_id.trim()), ("scope", SCOPE)]))
        .map_err(|e| describe("Starting Microsoft sign-in", e))?;
    let device_code = dc["device_code"].as_str().ok_or("Microsoft returned no device code")?.to_string();
    let user_code = dc["user_code"].as_str().unwrap_or_default().to_string();
    let page = dc["verification_uri"].as_str().unwrap_or("https://www.microsoft.com/link").to_string();
    let interval = dc["interval"].as_u64().unwrap_or(5).max(2);
    let deadline = Instant::now() + Duration::from_secs(dc["expires_in"].as_u64().unwrap_or(900));
    *shared.login_prompt.lock().unwrap() = Some((user_code, page.clone()));
    let _ = open::that(&page);

    let result = loop {
        std::thread::sleep(Duration::from_secs(interval));
        if shared.login_prompt.lock().unwrap().is_none() { break Err("Sign-in cancelled.".to_string()); }
        if Instant::now() > deadline { break Err("The sign-in code expired. Try again.".to_string()); }
        match body(agent().post(TOKEN_URL).send_form(&[
            ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"), ("client_id", client_id.trim()), ("device_code", &device_code),
        ])) {
            Ok(t) => break Ok(t),
            Err((_, v)) if v["error"] == "authorization_pending" || v["error"] == "slow_down" => continue,
            Err(e) => break Err(describe("Microsoft sign-in", e)),
        }
    };
    *shared.login_prompt.lock().unwrap() = None;
    let token = result?;
    shared.set_status("Signing in to Minecraft...");
    minecraft_login(token["access_token"].as_str().ok_or("Microsoft returned no access token")?, token["refresh_token"].as_str().unwrap_or_default().to_string())
}

/// Silent re-login with the stored refresh token (done before every launch).
pub fn refresh(client_id: &str, account: &Account) -> Res<Account> {
    if !account.access_token.is_empty() && cfg!(feature = "devtest") { return Ok(account.clone()); }
    let token = body(agent().post(TOKEN_URL).send_form(&[
        ("grant_type", "refresh_token"), ("client_id", client_id.trim()), ("refresh_token", &account.refresh_token), ("scope", SCOPE),
    ])).map_err(|e| format!("{} Please sign in again.", describe("Refreshing the Microsoft session", e)))?;
    let new_refresh = token["refresh_token"].as_str().unwrap_or(&account.refresh_token).to_string();
    minecraft_login(token["access_token"].as_str().ok_or("Microsoft returned no access token")?, new_refresh)
}
