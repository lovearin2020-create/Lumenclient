//! The launcher window.
use crate::state::{Account, Instance, Loader, Phase, Settings, Shared, INSTANCES};
use crate::{auth, content, paths};
use egui::{Align, Color32, FontFamily, FontId, Layout, RichText, Rounding, Stroke, Vec2};
use std::sync::atomic::Ordering;
use std::sync::Arc;
use std::time::Duration;

const BG: Color32 = Color32::from_rgb(0x0F, 0x11, 0x16);
const SIDE: Color32 = Color32::from_rgb(0x14, 0x17, 0x1D);
const CARD: Color32 = Color32::from_rgb(0x1A, 0x1D, 0x25);
const CARD_HI: Color32 = Color32::from_rgb(0x22, 0x27, 0x33);
const LINE: Color32 = Color32::from_rgb(0x2B, 0x31, 0x3D);
const TEXT: Color32 = Color32::from_rgb(0xF2, 0xF4, 0xF8);
const MUTED: Color32 = Color32::from_rgb(0x88, 0x91, 0xA5);
const ACCENT: Color32 = Color32::from_rgb(0x4C, 0x8D, 0xFF);
const GREEN: Color32 = Color32::from_rgb(0x2F, 0xBF, 0x71);
const RED: Color32 = Color32::from_rgb(0xE0, 0x52, 0x6A);

#[derive(PartialEq, Clone, Copy)]
enum Tab { Play, Mods, Profiles, Settings }

pub struct App {
    shared: Arc<Shared>,
    settings: Settings,
    selected: Instance,
    tab: Tab,
    optifine_path: String,
    note: String,
    new_profile: String,
    show_log: bool,
}

fn bold(size: f32) -> FontId { FontId::new(size, FontFamily::Name("bold".into())) }
fn logo(size: f32) -> FontId { FontId::new(size, FontFamily::Name("logo".into())) }

impl App {
    pub fn new(cc: &eframe::CreationContext<'_>) -> Self {
        let mut fonts = egui::FontDefinitions::default();
        fonts.font_data.insert("poppins".into(), egui::FontData::from_static(include_bytes!("../assets/Poppins-Medium.ttf")));
        fonts.font_data.insert("poppins-semibold".into(), egui::FontData::from_static(include_bytes!("../assets/Poppins-SemiBold.ttf")));
        fonts.font_data.insert("poppins-bold".into(), egui::FontData::from_static(include_bytes!("../assets/Poppins-Bold.ttf")));
        fonts.families.entry(FontFamily::Proportional).or_default().insert(0, "poppins".into());
        fonts.families.insert(FontFamily::Name("bold".into()), vec!["poppins-semibold".into(), "poppins".into()]);
        fonts.families.insert(FontFamily::Name("logo".into()), vec!["poppins-bold".into(), "poppins".into()]);
        cc.egui_ctx.set_fonts(fonts);

        let mut visuals = egui::Visuals::dark();
        visuals.panel_fill = BG;
        visuals.window_fill = SIDE;
        visuals.window_rounding = Rounding::same(10.0);
        visuals.window_stroke = Stroke::new(1.0_f32, LINE);
        visuals.extreme_bg_color = Color32::from_rgb(0x0B, 0x0D, 0x11);
        visuals.selection.bg_fill = ACCENT.linear_multiply(0.6);
        for w in [&mut visuals.widgets.inactive, &mut visuals.widgets.hovered, &mut visuals.widgets.active, &mut visuals.widgets.noninteractive, &mut visuals.widgets.open] {
            w.rounding = Rounding::same(6.0);
        }
        visuals.widgets.noninteractive.fg_stroke.color = TEXT;
        visuals.widgets.inactive.fg_stroke.color = TEXT;
        visuals.widgets.hovered.fg_stroke.color = Color32::WHITE;
        visuals.widgets.active.fg_stroke.color = Color32::WHITE;
        visuals.widgets.inactive.weak_bg_fill = CARD_HI;
        visuals.widgets.inactive.bg_fill = CARD_HI;
        visuals.widgets.hovered.weak_bg_fill = Color32::from_rgb(0x30, 0x36, 0x44);
        visuals.widgets.hovered.bg_stroke = Stroke::new(1.0_f32, ACCENT);
        visuals.widgets.active.weak_bg_fill = ACCENT;
        cc.egui_ctx.set_visuals(visuals);
        let mut style = (*cc.egui_ctx.style()).clone();
        style.spacing.item_spacing = Vec2::new(8.0, 8.0);
        style.spacing.button_padding = Vec2::new(12.0, 6.0);
        style.text_styles.insert(egui::TextStyle::Body, FontId::proportional(14.0));
        style.text_styles.insert(egui::TextStyle::Button, FontId::proportional(14.0));
        style.text_styles.insert(egui::TextStyle::Small, FontId::proportional(12.0));
        cc.egui_ctx.set_style(style);

        let settings = Settings::load();
        let shared = Arc::new(Shared::new());
        *shared.account.lock().unwrap() = auth::load_account();
        let selected = *INSTANCES.iter().find(|i| i.id == settings.last_instance).unwrap_or(&INSTANCES[0]);
        #[allow(unused_mut)]
        let mut tab = Tab::Play;
        #[cfg(feature = "devtest")]
        if let Ok(t) = std::env::var("LUMEN_TAB") { tab = match t.as_str() { "mods" => Tab::Mods, "profiles" => Tab::Profiles, "settings" => Tab::Settings, _ => Tab::Play }; }
        App { shared, settings, selected, tab, optifine_path: String::new(), note: String::new(), new_profile: String::new(), show_log: false }
    }

    fn busy(&self) -> bool { matches!(self.shared.phase(), Phase::Working | Phase::Running) }

    fn sidebar(&mut self, ui: &mut egui::Ui) {
        ui.add_space(18.0);
        ui.horizontal(|ui| {
            ui.add_space(6.0);
            ui.label(RichText::new("LUMEN").font(logo(30.0)).color(TEXT).extra_letter_spacing(3.0));
        });
        ui.horizontal(|ui| {
            ui.add_space(8.0);
            ui.label(RichText::new("CLIENT").font(bold(11.0)).color(ACCENT).extra_letter_spacing(5.0));
        });
        ui.add_space(22.0);
        ui.label(RichText::new("VERSIONS").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
        for inst in INSTANCES {
            let sel = inst == self.selected;
            let (rect, resp) = ui.allocate_exact_size(Vec2::new(ui.available_width(), 54.0), egui::Sense::click());
            let fill = if sel { ACCENT.linear_multiply(0.22) } else if resp.hovered() { CARD_HI } else { CARD };
            ui.painter().rect(rect, Rounding::same(8.0), fill, Stroke::new(1.0_f32, if sel { ACCENT } else { LINE }));
            ui.painter().text(rect.left_top() + Vec2::new(14.0, 8.0), egui::Align2::LEFT_TOP, inst.title, bold(17.0), TEXT);
            ui.painter().text(rect.left_top() + Vec2::new(14.0, 31.0), egui::Align2::LEFT_TOP, inst.subtitle, FontId::proportional(11.5), MUTED);
            if resp.clicked() && !self.busy() {
                self.selected = inst;
                self.settings.last_instance = inst.id.to_string();
                self.settings.save();
                self.note.clear();
            }
        }

        ui.with_layout(Layout::bottom_up(Align::Min), |ui| {
            ui.add_space(14.0);
            let account = self.shared.account.lock().unwrap().clone();
            egui::Frame::none().fill(CARD).rounding(Rounding::same(8.0)).stroke(Stroke::new(1.0_f32, LINE)).inner_margin(egui::Margin::same(12.0)).show(ui, |ui| {
                ui.set_width(ui.available_width());
                // This panel is laid out bottom-up, so widgets are added in reverse reading order.
                match account {
                    Some(a) => {
                        if ui.add_enabled(!self.busy(), egui::Button::new(RichText::new("Sign out").size(12.0))).clicked() {
                            auth::sign_out();
                            *self.shared.account.lock().unwrap() = None;
                        }
                        ui.label(RichText::new(&a.name).font(bold(16.0)));
                        ui.label(RichText::new("SIGNED IN").font(bold(10.0)).color(GREEN).extra_letter_spacing(1.5));
                    }
                    None => {
                        let btn = egui::Button::new(RichText::new("Sign in with Microsoft").font(bold(13.0)).color(Color32::WHITE)).fill(ACCENT).min_size(Vec2::new(ui.available_width(), 32.0));
                        if ui.add_enabled(!self.busy(), btn).clicked() { self.start_sign_in(); }
                        ui.label(RichText::new("NOT SIGNED IN").font(bold(10.0)).color(MUTED).extra_letter_spacing(1.5));
                    }
                }
            });
        });
    }

    fn start_sign_in(&mut self) {
        if self.settings.client_id.trim().is_empty() {
            self.tab = Tab::Settings;
            self.note = "Sign-in needs your Azure application ID. Enter it below first.".into();
            return;
        }
        let (shared, id) = (self.shared.clone(), self.settings.client_id.clone());
        shared.set_phase(Phase::Working);
        shared.set_status("Waiting for Microsoft sign-in...");
        std::thread::spawn(move || match auth::sign_in(&id, &shared) {
            Ok(a) => { *shared.account.lock().unwrap() = Some(a); shared.set_status("Signed in"); shared.set_phase(Phase::Idle); }
            Err(e) => { shared.log(format!("ERROR: {e}")); shared.set_phase(Phase::Failed(e)); }
        });
    }

    fn tabs(&mut self, ui: &mut egui::Ui) {
        ui.horizontal(|ui| {
            for (t, label) in [(Tab::Play, "Play"), (Tab::Mods, "Mods"), (Tab::Profiles, "Profiles"), (Tab::Settings, "Settings")] {
                let sel = self.tab == t;
                let text = RichText::new(label).font(bold(15.0)).color(if sel { TEXT } else { MUTED });
                let resp = ui.add(egui::Button::new(text).frame(false));
                if sel {
                    let r = resp.rect;
                    ui.painter().rect_filled(egui::Rect::from_min_max(egui::pos2(r.left() + 10.0, r.bottom() + 1.0), egui::pos2(r.right() - 10.0, r.bottom() + 3.0)), Rounding::same(1.0), ACCENT);
                }
                if resp.clicked() { self.tab = t; self.note.clear(); }
                ui.add_space(6.0);
            }
        });
        ui.add_space(6.0);
        let r = ui.available_rect_before_wrap();
        ui.painter().hline(r.x_range(), r.top(), Stroke::new(1.0_f32, LINE));
        ui.add_space(14.0);
    }

    fn card<R>(ui: &mut egui::Ui, add: impl FnOnce(&mut egui::Ui) -> R) -> R {
        egui::Frame::none().fill(CARD).rounding(Rounding::same(8.0)).inner_margin(egui::Margin::same(14.0)).show(ui, |ui| {
            ui.set_width(ui.available_width());
            add(ui)
        }).inner
    }

    fn play_tab(&mut self, ui: &mut egui::Ui) {
        let inst = self.selected;
        ui.label(RichText::new(format!("Minecraft {}", inst.mc)).font(logo(34.0)));
        ui.label(RichText::new(match inst.loader {
            Loader::Forge => "Forge 11.15.1.2318 with the Lumen PvP client",
            Loader::Fabric => "Fabric with Sodium for high frame rates",
        }).color(MUTED).size(15.0));
        ui.add_space(8.0);

        Self::card(ui, |ui| {
            ui.label(RichText::new("INCLUDED").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
            let mods = content::list_mods(&inst);
            let has = |needle: &str| mods.iter().any(|m| m.enabled && m.file.to_lowercase().contains(needle));
            let row = |ui: &mut egui::Ui, name: &str, state: &str, ok: bool| {
                ui.horizontal(|ui| {
                    let (dot, _) = ui.allocate_exact_size(Vec2::new(10.0, 14.0), egui::Sense::hover());
                    if ok { ui.painter().circle_filled(dot.center(), 4.0, GREEN); } else { ui.painter().circle_stroke(dot.center(), 4.0, Stroke::new(1.5_f32, MUTED)); }
                    ui.label(RichText::new(name).font(bold(14.0)));
                    ui.label(RichText::new(state).color(MUTED).size(13.0));
                });
            };
            match inst.loader {
                Loader::Forge => {
                    row(ui, &format!("Lumen Client {}", content::LUMEN_VERSION), "built in, installed on first launch", true);
                    if has("optifine") { row(ui, "OptiFine", "installed", true); }
                    else if content::find_optifine().is_some() { row(ui, "OptiFine", "found on this PC, will be copied on launch", true); }
                    else { row(ui, "OptiFine", "not found - add your OptiFine 1.8.9 jar in the Mods tab", false); }
                }
                Loader::Fabric => {
                    row(ui, "Fabric Loader", "latest stable, installed on first launch", true);
                    row(ui, "Sodium", if has("sodium") { "installed" } else { "downloaded from Modrinth on first launch" }, true);
                }
            }
        });
        ui.add_space(10.0);

        let phase = self.shared.phase();
        let signed_in = self.shared.account.lock().unwrap().is_some();
        ui.horizontal(|ui| {
            let (label, color) = match &phase {
                Phase::Working => ("WORKING...", CARD_HI),
                Phase::Running => ("RUNNING", CARD_HI),
                _ => ("PLAY", GREEN),
            };
            let btn = egui::Button::new(RichText::new(label).font(logo(22.0)).color(Color32::WHITE)).fill(color).rounding(Rounding::same(10.0)).min_size(Vec2::new(220.0, 58.0));
            if ui.add_enabled(!self.busy(), btn).clicked() {
                if !signed_in { self.start_sign_in(); } else {
                    let (inst, settings, shared) = (inst, self.settings.clone(), self.shared.clone());
                    std::thread::spawn(move || crate::play(inst, settings, shared));
                }
            }
            ui.vertical(|ui| {
                ui.add_space(6.0);
                let status = self.shared.status.lock().unwrap().clone();
                match &phase {
                    Phase::Failed(e) => { ui.label(RichText::new(e).color(RED)); }
                    _ if !signed_in => { ui.label(RichText::new("Sign in with Microsoft to play.").color(MUTED)); }
                    _ => { ui.label(RichText::new(if status.is_empty() { "Ready".into() } else { status }).color(MUTED)); }
                }
                let total = self.shared.total.load(Ordering::SeqCst);
                if total > 0 {
                    let done = self.shared.done.load(Ordering::SeqCst);
                    ui.add(egui::ProgressBar::new(done as f32 / total as f32).desired_width(360.0).fill(ACCENT));
                }
            });
        });
        ui.add_space(10.0);
        ui.horizontal(|ui| {
            if ui.button("Open game folder").clicked() {
                let dir = paths::instance(inst.id);
                let _ = std::fs::create_dir_all(&dir);
                let _ = open::that(dir);
            }
            if ui.button(if self.show_log { "Hide log" } else { "Show log" }).clicked() { self.show_log = !self.show_log; }
        });
        if self.show_log {
            egui::Frame::none().fill(Color32::from_rgb(0x0B, 0x0D, 0x11)).rounding(Rounding::same(8.0)).inner_margin(egui::Margin::same(10.0)).show(ui, |ui| {
                egui::ScrollArea::vertical().stick_to_bottom(true).auto_shrink([false, false]).show(ui, |ui| {
                    for line in self.shared.log.lock().unwrap().iter() {
                        ui.label(RichText::new(line).font(FontId::monospace(11.5)).color(if line.starts_with("ERROR") { RED } else { MUTED }));
                    }
                });
            });
        }
    }

    fn mods_tab(&mut self, ui: &mut egui::Ui) {
        let inst = self.selected;
        ui.horizontal(|ui| {
            ui.label(RichText::new(format!("Mods for {}", inst.mc)).font(bold(20.0)));
            ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                if ui.button("Open mods folder").clicked() {
                    let dir = paths::instance(inst.id).join("mods");
                    let _ = std::fs::create_dir_all(&dir);
                    let _ = open::that(dir);
                }
                if ui.add_enabled(!self.busy(), egui::Button::new("Import settings from official launcher")).clicked() {
                    self.note = match content::import_settings(&inst) {
                        Ok(n) => format!("Copied {n} settings files from {}", paths::official_minecraft().display()),
                        Err(e) => e,
                    };
                }
            });
        });
        ui.label(RichText::new("Drop any extra mod jars into the mods folder. Unticking a mod keeps the file but stops it loading.").color(MUTED).size(13.0));
        if !self.note.is_empty() { ui.label(RichText::new(&self.note).color(ACCENT)); }

        if inst.loader == Loader::Forge {
            Self::card(ui, |ui| {
                ui.label(RichText::new("OPTIFINE").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
                ui.label(RichText::new("OptiFine cannot be bundled or downloaded automatically (its license forbids redistribution). Download the 1.8.9 jar yourself, then import it here.").color(MUTED).size(13.0));
                ui.horizontal_wrapped(|ui| {
                    ui.add(egui::TextEdit::singleline(&mut self.optifine_path).hint_text("Path to OptiFine_1.8.9_....jar").desired_width(250.0));
                    if ui.button("Import").clicked() {
                        let p = self.optifine_path.trim().trim_matches('"').to_string();
                        self.note = match content::import_optifine(&inst, std::path::Path::new(&p)) { Ok(n) => format!("Imported {n}"), Err(e) => e };
                    }
                    if ui.button("Find on this PC").clicked() {
                        match content::find_optifine() {
                            Some(p) => self.optifine_path = p.to_string_lossy().to_string(),
                            None => self.note = "No OptiFine 1.8.9 jar found in the official mods folder or Downloads.".into(),
                        }
                    }
                    if ui.button("Download page").clicked() { let _ = open::that("https://optifine.net/downloads"); }
                });
            });
        }

        let mods = content::list_mods(&inst);
        if mods.is_empty() {
            ui.add_space(20.0);
            ui.label(RichText::new("Nothing installed yet. The default mods are added the first time you press Play.").color(MUTED));
        }
        egui::ScrollArea::vertical().auto_shrink([false, false]).show(ui, |ui| {
            for m in mods {
                egui::Frame::none().fill(CARD).rounding(Rounding::same(8.0)).inner_margin(egui::Margin::symmetric(14.0, 9.0)).show(ui, |ui| {
                    ui.set_width(ui.available_width());
                    ui.horizontal(|ui| {
                        let mut on = m.enabled;
                        if ui.add_enabled(!self.busy(), egui::Checkbox::new(&mut on, "")).changed() {
                            if let Err(e) = content::toggle_mod(&inst, &m.file) { self.note = e; }
                        }
                        ui.label(RichText::new(m.file.trim_end_matches(".disabled")).font(bold(14.0)).color(if m.enabled { TEXT } else { MUTED }));
                        ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                            ui.label(RichText::new(format!("{:.1} MB", m.size as f64 / 1_048_576.0)).color(MUTED).size(12.0));
                        });
                    });
                });
            }
        });
    }

    fn profiles_tab(&mut self, ui: &mut egui::Ui) {
        let inst = self.selected;
        ui.label(RichText::new("Settings profiles").font(bold(20.0)));
        if inst.loader != Loader::Forge {
            ui.label(RichText::new("Lumen settings profiles belong to the 1.8.9 version. Select 1.8.9 on the left to manage them.").color(MUTED));
            return;
        }
        ui.label(RichText::new("A profile is a full copy of your Lumen setup: which mods are on, every setting and the HUD layout. You can also save and load them in game (RSHIFT > Profiles).").color(MUTED).size(13.0));
        if !self.note.is_empty() { ui.label(RichText::new(&self.note).color(ACCENT)); }
        Self::card(ui, |ui| {
            ui.horizontal(|ui| {
                ui.add(egui::TextEdit::singleline(&mut self.new_profile).hint_text("New profile name").desired_width(240.0));
                if ui.add(egui::Button::new(RichText::new("Save current settings").color(Color32::WHITE)).fill(ACCENT)).clicked() {
                    self.note = match content::save_current_as(&inst, &self.new_profile) { Ok(()) => { self.new_profile.clear(); "Profile saved.".into() } Err(e) => e };
                }
                if ui.button("Open profiles folder").clicked() {
                    let dir = paths::instance(inst.id).join("config").join("lumen").join("profiles");
                    let _ = std::fs::create_dir_all(&dir);
                    let _ = open::that(dir);
                }
            });
        });
        let active = content::active_profile(&inst);
        let list = content::list_profiles(&inst);
        if list.is_empty() { ui.add_space(16.0); ui.label(RichText::new("No profiles yet.").color(MUTED)); }
        egui::ScrollArea::vertical().auto_shrink([false, false]).show(ui, |ui| {
            for name in list {
                let is_active = name == active;
                egui::Frame::none().fill(CARD).rounding(Rounding::same(8.0)).stroke(Stroke::new(1.0_f32, if is_active { GREEN } else { CARD })).inner_margin(egui::Margin::symmetric(14.0, 9.0)).show(ui, |ui| {
                    ui.set_width(ui.available_width());
                    ui.horizontal(|ui| {
                        ui.label(RichText::new(&name).font(bold(15.0)));
                        if is_active { ui.label(RichText::new("ACTIVE").font(bold(10.0)).color(GREEN).extra_letter_spacing(1.5)); }
                        ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                            if ui.add_enabled(!self.busy(), egui::Button::new("Delete")).clicked() {
                                self.note = match content::delete_profile(&inst, &name) { Ok(()) => format!("Deleted {name}."), Err(e) => e };
                            }
                            if ui.add_enabled(!self.busy(), egui::Button::new("Use this profile")).clicked() {
                                self.note = match content::activate_profile(&inst, &name) { Ok(()) => format!("{name} will be used the next time the game starts."), Err(e) => e };
                            }
                        });
                    });
                });
            }
        });
    }

    fn settings_tab(&mut self, ui: &mut egui::Ui) {
        let mut changed = false;
        ui.label(RichText::new("Settings").font(bold(20.0)));
        if !self.note.is_empty() { ui.label(RichText::new(&self.note).color(ACCENT)); }
        egui::ScrollArea::vertical().auto_shrink([false, false]).show(ui, |ui| {
            Self::card(ui, |ui| {
                ui.label(RichText::new("MEMORY").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
                ui.horizontal(|ui| {
                    changed |= ui.add(egui::Slider::new(&mut self.settings.memory_mb, 1024..=8192).step_by(256.0).suffix(" MB")).changed();
                    ui.label(RichText::new("2-4 GB is plenty for 1.8.9 PvP").color(MUTED).size(12.5));
                });
                ui.add_space(4.0);
                ui.label(RichText::new("EXTRA JAVA ARGUMENTS").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
                changed |= ui.add(egui::TextEdit::singleline(&mut self.settings.extra_jvm_args).hint_text("optional").desired_width(f32::INFINITY)).changed();
            });
            Self::card(ui, |ui| {
                ui.label(RichText::new("MICROSOFT SIGN-IN").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
                ui.label(RichText::new("Signing in inside this app needs an Azure application ID registered by you and approved by Mojang for the Minecraft API. Until Mojang approves it, sign-in stops with an HTTP 403 message.").color(MUTED).size(13.0));
                ui.horizontal(|ui| {
                    ui.label("Application (client) ID");
                    changed |= ui.add(egui::TextEdit::singleline(&mut self.settings.client_id).hint_text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx").desired_width(330.0)).changed();
                });
                ui.label(RichText::new("1. Register an app (personal Microsoft accounts only) and turn on \"Allow public client flows\".\n2. Paste its Application (client) ID above and try to sign in once.\n3. Send the ID to Mojang with the review form and wait for approval.").color(MUTED).size(13.0));
                ui.horizontal(|ui| {
                    if ui.button("Azure app registrations").clicked() { let _ = open::that("https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade"); }
                    if ui.button("Mojang review form").clicked() { let _ = open::that("https://aka.ms/mce-reviewappid"); }
                });
            });
            Self::card(ui, |ui| {
                ui.label(RichText::new("FILES").font(bold(11.0)).color(MUTED).extra_letter_spacing(1.5));
                ui.label(RichText::new(paths::root().to_string_lossy()).color(MUTED).size(13.0));
                ui.label(RichText::new("Game files, each version's mods and config, and your sign-in token are stored here. Copy this folder to move everything to another PC.").color(MUTED).size(13.0));
                if ui.button("Open data folder").clicked() {
                    let _ = std::fs::create_dir_all(paths::root());
                    let _ = open::that(paths::root());
                }
            });
        });
        if changed { self.settings.save(); }
    }

    fn login_window(&mut self, ctx: &egui::Context) {
        let prompt = self.shared.login_prompt.lock().unwrap().clone();
        if let Some((code, page)) = prompt {
            egui::Window::new("Sign in with Microsoft").collapsible(false).resizable(false).anchor(egui::Align2::CENTER_CENTER, Vec2::ZERO).show(ctx, |ui| {
                ui.set_width(380.0);
                ui.label(RichText::new("A browser page has opened. Enter this code there, then come back:").color(MUTED));
                ui.add_space(6.0);
                ui.vertical_centered(|ui| ui.label(RichText::new(&code).font(logo(34.0)).color(ACCENT).extra_letter_spacing(4.0)));
                ui.add_space(6.0);
                ui.horizontal(|ui| {
                    if ui.button("Copy code").clicked() { ui.output_mut(|o| o.copied_text = code.clone()); }
                    if ui.button("Open page again").clicked() { let _ = open::that(&page); }
                    if ui.button("Cancel").clicked() { *self.shared.login_prompt.lock().unwrap() = None; }
                });
                ui.label(RichText::new(&page).color(MUTED).size(12.0));
            });
        }
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        if self.busy() || self.shared.login_prompt.lock().unwrap().is_some() { ctx.request_repaint_after(Duration::from_millis(150)); }
        else { ctx.request_repaint_after(Duration::from_secs(2)); }

        egui::SidePanel::left("side").exact_width(250.0).resizable(false)
            .frame(egui::Frame::none().fill(SIDE).inner_margin(egui::Margin::symmetric(16.0, 0.0)))
            .show(ctx, |ui| self.sidebar(ui));
        egui::CentralPanel::default().frame(egui::Frame::none().fill(BG).inner_margin(egui::Margin::symmetric(26.0, 18.0))).show(ctx, |ui| {
            self.tabs(ui);
            match self.tab {
                Tab::Play => self.play_tab(ui),
                Tab::Mods => self.mods_tab(ui),
                Tab::Profiles => self.profiles_tab(ui),
                Tab::Settings => self.settings_tab(ui),
            }
        });
        self.login_window(ctx);
    }
}

#[allow(dead_code)]
fn _unused(_: Account) {}
