package dev.lumen.gui;

import dev.lumen.Lumen;
import dev.lumen.core.Category;
import dev.lumen.core.Config;
import dev.lumen.core.HudModule;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The RSHIFT mod menu: category sidebar, searchable module grid and per-module settings. */
public class ModMenuScreen extends GuiScreen {
    private static final int W = 452, H = 276, SIDE = 108, CARD_H = 46, GAP = 8, ROW = 26;
    private static final int BG = 0xF20F1116, SIDE_BG = 0xFF14171D, CARD = 0xFF1A1D25, CARD_HOVER = 0xFF222733;
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, OFF = 0xFF2B303B;

    private static Category category; // null = all mods
    private Module open;
    private String search = "";
    private boolean searching;
    private float scroll, scrollTo, settingsScroll, settingsScrollTo;
    private Setting.Num dragging;
    private Setting.Text editing;
    private boolean profiles;
    private final Setting.Text profileName = new Setting.Text("Profile name", "", 24);
    private String profileNote = "";
    private float dragX, dragW;
    private final long opened = System.currentTimeMillis();
    private final Map<Object, Float> anim = new HashMap<Object, Float>();

    private int px, py;

    public void initGui() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        Keyboard.enableRepeatEvents(true);
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Config.save();
    }

    public boolean doesGuiPauseGame() { return false; }

    private float anim(Object key, float target) {
        Float cur = anim.get(key);
        float v = Util.lerp(cur == null ? target : cur, target, 0.25f);
        anim.put(key, v);
        return v;
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<Module>();
        String q = search.toLowerCase();
        for (Module m : Lumen.modules.all) {
            if (q.isEmpty() ? (category == null || m.category == category) : m.name.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    // ---------------------------------------------------------------- drawing

    /** Shrinks the panel on small windows / large GUI scales so it always fits on screen. */
    private float fit() { return Math.min(1f, Math.min((width - 12) / (float) W, (height - 12) / (float) H)); }
    private int tx(int mx) { return Math.round((mx - width / 2f) / fit() + width / 2f); }
    private int ty(int my) { return Math.round((my - height / 2f) / fit() + height / 2f); }

    private void clip(float x, float y, float w, float h) {
        float f = fit(), cx = width / 2f, cy = height / 2f;
        R.scissor((x - cx) * f + cx, (y - cy) * f + cy, w * f, h * f);
    }

    public void drawScreen(int mx, int my, float pt) {
        mx = tx(mx); my = ty(my);
        Fnt.transform(fit(), width / 2f, height / 2f);
        float t = Math.min(1f, (System.currentTimeMillis() - opened) / 160f);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        if (mc.theWorld == null) { Fnt.transform(1f, 0, 0); Backdrop.draw(width, height, mx, my); Fnt.transform(fit(), width / 2f, height / 2f); }
        R.rect(0, 0, width, height, R.alpha(mc.theWorld == null ? 0x55000000 : 0x99000000, ease));

        GlStateManager.pushMatrix();
        float s = (0.94f + 0.06f * ease) * fit();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(s, s, 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        R.roundRect(px, py, W, H, 9, BG);
        R.roundRect(px, py, SIDE, H, 9, SIDE_BG);
        R.rect(px + SIDE - 9, py, 9, H, SIDE_BG);
        R.rect(px + SIDE, py, 1, H, 0xFF20242D);
        R.roundOutline(px, py, W, H, 9, 1f, 0xFF262B36);

        drawSidebar(mx, my);
        if (profiles) drawProfiles(mx, my); else if (open == null) drawGrid(mx, my); else drawSettings(mx, my);
        GlStateManager.popMatrix();
        R.reset();
    }

    private void drawSidebar(int mx, int my) {
        R.glow(px + 26, py + 26, 34, 0x334C8DFF);
        Fnt.draw("LUMEN", px + 15, py + 13, 17, Fnt.LOGO, 2.5f, 0xFFFFFFFF, 0xFF7FB2FF);
        Fnt.draw("CLIENT", px + 16, py + 35, 7, Fnt.BOLD, 3.5f, R.ACCENT, R.ACCENT);
        String ver = "v" + Lumen.VERSION;
        Fnt.draw(ver, px + SIDE - 12 - Fnt.width(ver, 7), py + 35, 7, 0xFF5C6475);

        int y = py + 54;
        for (int i = -1; i < Category.values().length; i++) {
            Category c = i < 0 ? null : Category.values()[i];
            boolean sel = !profiles && open == null && search.isEmpty() && category == c;
            boolean hov = R.in(mx, my, px + 8, y, SIDE - 16, 20);
            float a = anim("cat" + i, sel ? 1f : hov ? 0.35f : 0f);
            if (a > 0.01f) R.roundRect(px + 8, y, SIDE - 16, 20, 5, R.alpha(R.ACCENT, a * 0.9f));
            Fnt.draw(c == null ? "All Mods" : c.label, px + 15, y + 5.5f, 9, sel ? 0xFFFFFFFF : R.mix(MUTED, TEXT, a * 2));
            String n = String.valueOf(Lumen.modules.of(c).size());
            Fnt.draw(n, px + SIDE - 14 - Fnt.width(n, 8), y + 6, 8, sel ? 0xDDFFFFFF : 0xFF5C6475);
            y += 22;
        }

        boolean ph = R.in(mx, my, px + 8, py + H - 84, SIDE - 16, 20);
        R.roundRect(px + 8, py + H - 84, SIDE - 16, 20, 5, profiles ? R.alpha(R.ACCENT, 0.9f) : ph ? 0xFF283041 : 0xFF1A1D25);
        Fnt.center(Fnt.fit(Config.activeProfile.isEmpty() ? "Profiles" : "Profile: " + Config.activeProfile, 8, SIDE - 24), px + SIDE / 2f, py + H - 78.5f, 8, profiles || ph ? TEXT : MUTED);

        boolean fh = R.in(mx, my, px + 8, py + H - 60, SIDE - 16, 20);
        R.roundRect(px + 8, py + H - 60, SIDE - 16, 20, 5, fh ? 0xFF283041 : 0xFF1A1D25);
        Fnt.center(Fnt.minecraft ? "Font: Minecraft" : "Font: Smooth", px + SIDE / 2f, py + H - 54.5f, 8, fh ? TEXT : MUTED);

        boolean hov = R.in(mx, my, px + 8, py + H - 34, SIDE - 16, 24);
        R.roundRect(px + 8, py + H - 34, SIDE - 16, 24, 6, hov ? 0xFF283041 : 0xFF1D222C);
        R.roundOutline(px + 8, py + H - 34, SIDE - 16, 24, 6, 1f, hov ? R.ACCENT : 0xFF2B313D);
        Fnt.center("Edit HUD Layout", px + SIDE / 2f, py + H - 26.5f, 9, TEXT);
    }

    private void drawGrid(int mx, int my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        Fnt.drawB(search.isEmpty() ? (category == null ? "All Mods" : category.label) : "Search", x0, y0 + 1, 13, TEXT);

        // search box
        int sw = 120, sx = x0 + w - sw;
        R.roundRect(sx, y0, sw, 18, 5, 0xFF1A1D25);
        R.roundOutline(sx, y0, sw, 18, 5, 1f, searching ? R.ACCENT : 0xFF2B313D);
        String shown = search.isEmpty() && !searching ? "Search mods..." : Fnt.fit(search, 9, sw - 18) + (searching && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        Fnt.draw(shown, sx + 7, y0 + 4.5f, 9, search.isEmpty() && !searching ? 0xFF5C6475 : TEXT);

        List<Module> list = visible();
        int top = y0 + 30, viewH = H - 44 - 30;
        int cw = (w - GAP * 2) / 3;
        int rows = (list.size() + 2) / 3;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - viewH);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Util.lerp(scroll, scrollTo, 0.3f);

        Module hovered = null;
        if (list.isEmpty()) Fnt.center("No mods found", x0 + w / 2f, top + 40, 10, MUTED);

        clip(x0 - 2, top, w + 4, viewH);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > top + viewH) continue;
            boolean inView = my >= top && my < top + viewH;
            boolean hov = inView && R.in(mx, my, cx, cy, cw, CARD_H);
            float h = anim(m, hov ? 1 : 0);
            R.roundRect(cx, cy, cw, CARD_H, 6, R.mix(CARD, CARD_HOVER, h));
            if (m.isEnabled()) R.roundOutline(cx, cy, cw, CARD_H, 6, 1f, R.alpha(GREEN, 0.55f));
            Fnt.centerB(Fnt.fit(m.name, 10, cw - 10), cx + cw / 2f, cy + 7, 10, TEXT);
            if (hov) hovered = m;

            boolean gear = !m.settings.isEmpty();
            float bw = cw - 12 - (gear ? 20 : 0), bx = cx + 6, by = cy + CARD_H - 22;
            float e = anim(m.name + "#on", m.isEnabled() ? 1 : 0);
            boolean bh = inView && R.in(mx, my, bx, by, bw, 16);
            R.roundRect(bx, by, bw, 16, 4, R.mix(bh ? 0xFF353B48 : OFF, bh ? 0xFF38D283 : GREEN, e));
            Fnt.center(m.isEnabled() ? "ENABLED" : "DISABLED", bx + bw / 2f, by + 4.5f, 8, m.isEnabled() ? 0xFFFFFFFF : 0xFFB3BACB);
            if (gear) {
                float gx = cx + cw - 22;
                boolean gh = inView && R.in(mx, my, gx, by, 16, 16);
                R.roundRect(gx, by, 16, 16, 4, gh ? 0xFF3A4252 : OFF);
                for (int k = 0; k < 3; k++) {
                    R.rect(gx + 4, by + 4.5f + k * 3, 8, 1, 0xFFB3BACB);
                    R.rect(gx + (k == 1 ? 9 : 5), by + 4 + k * 3, 2, 2, 0xFFFFFFFF);
                }
            }
        }
        R.endScissor();
        R.rect(x0, py + H - 24, w, 1, 0xFF20242D);
        Fnt.draw(Fnt.fit(hovered != null ? hovered.name + "  -  " + hovered.description : "Right-click a mod to open its settings.", 8, w), x0, py + H - 17, 8, hovered != null ? 0xFFB3BACB : 0xFF5C6475);

        if (max > 0) {
            float trackH = viewH, barH = Math.max(18, trackH * viewH / (viewH + max));
            R.roundRect(px + W - 8, top + (trackH - barH) * (scroll / max), 3, barH, 1.5f, 0xFF333A48);
        }
    }

    private void drawProfiles(int mx, int my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        Fnt.drawB("Profiles", x0, y0 + 1, 13, TEXT);
        Fnt.draw("Each profile stores every mod's on/off state, settings and HUD layout.", x0, y0 + 24, 8, MUTED);

        // new profile row
        float ry = y0 + 40;
        R.roundRect(x0, ry, w, ROW - 4, 5, CARD);
        boolean focus = editing == profileName;
        R.roundRect(x0 + 8, ry + 3, 170, 16, 4, 0xFF262B36);
        R.roundOutline(x0 + 8, ry + 3, 170, 16, 4, 1f, focus ? R.ACCENT : 0xFF3A4150);
        String shown = profileName.value + (focus && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        if (shown.isEmpty()) Fnt.draw("New profile name...", x0 + 14, ry + 7, 8, 0xFF5C6475); else Fnt.draw(shown, x0 + 14, ry + 7, 8, TEXT);
        boolean sh = R.in(mx, my, x0 + w - 118, ry + 3, 110, 16);
        R.roundRect(x0 + w - 118, ry + 3, 110, 16, 4, sh ? 0xFF6AA0FF : R.ACCENT);
        Fnt.center("Save Current Settings", x0 + w - 63, ry + 7, 8, 0xFFFFFFFF);
        if (!profileNote.isEmpty()) Fnt.draw(profileNote, x0 + 186, ry + 7, 8, MUTED);

        java.util.List<String> list = Config.profiles();
        float y = ry + ROW + 4;
        if (list.isEmpty()) Fnt.center("No profiles yet. Type a name above and press Save.", x0 + w / 2f, y + 30, 9, MUTED);
        for (String name : list) {
            if (y + ROW > py + H - 10) break;
            boolean active = name.equals(Config.activeProfile);
            R.roundRect(x0, y, w, ROW - 4, 5, CARD);
            if (active) R.roundOutline(x0, y, w, ROW - 4, 5, 1f, R.alpha(GREEN, 0.6f));
            Fnt.draw(name, x0 + 10, y + 6.5f, 9, TEXT);
            if (active) Fnt.draw("ACTIVE", x0 + 16 + Fnt.width(name, 9), y + 7.5f, 7, GREEN);
            String[] labels = {"Load", "Overwrite", "Delete"};
            float bx = x0 + w - 8;
            for (int i = labels.length - 1; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                boolean hov = R.in(mx, my, bx, y + 3, bw, 16);
                R.roundRect(bx, y + 3, bw, 16, 4, hov ? (i == 2 ? 0xFFE0526A : i == 0 ? R.ACCENT : 0xFF3A4252) : 0xFF262B36);
                Fnt.center(labels[i], bx + bw / 2f, y + 7, 8, TEXT);
                bx -= 4;
            }
            y += ROW;
        }
    }

    private boolean clickProfiles(int mx, int my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        float ry = y0 + 40;
        if (R.in(mx, my, x0 + 8, ry + 3, 170, 16)) { editing = profileName; return true; }
        if (R.in(mx, my, x0 + w - 118, ry + 3, 110, 16)) {
            if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } else profileNote = "Enter a name first.";
            return true;
        }
        float y = ry + ROW + 4;
        for (String name : Config.profiles()) {
            if (y + ROW > py + H - 10) break;
            float bx = x0 + w - 8;
            for (int i = 2; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                if (R.in(mx, my, bx, y + 3, bw, 16)) {
                    if (i == 0) { Config.loadProfile(name); profileNote = "Loaded " + name + "."; }
                    else if (i == 1) { Config.saveProfile(name); profileNote = "Updated " + name + "."; }
                    else { Config.deleteProfile(name); profileNote = "Deleted " + name + "."; }
                    return true;
                }
                bx -= 4;
            }
            y += ROW;
        }
        return false;
    }

    private void drawSettings(int mx, int my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        boolean bh = R.in(mx, my, x0, y0, 44, 18);
        R.roundRect(x0, y0, 44, 18, 5, bh ? 0xFF283041 : 0xFF1A1D25);
        Fnt.center("< Back", x0 + 22, y0 + 4.5f, 9, TEXT);
        Fnt.drawB(open.name, x0 + 54, y0 + 1, 13, TEXT);

        // master toggle
        float e = anim(open.name + "#on", open.isEnabled() ? 1 : 0);
        float tx = x0 + w - 64;
        R.roundRect(tx, y0, 64, 18, 5, R.mix(OFF, GREEN, e));
        Fnt.center(open.isEnabled() ? "ENABLED" : "DISABLED", tx + 32, y0 + 5, 8, 0xFFFFFFFF);

        Fnt.draw(Fnt.fit(open.description, 8, w), x0, y0 + 26, 8, MUTED);

        int top = y0 + 42, viewH = H - 56 - 14;
        float max = Math.max(0, open.settings.size() * ROW + (open instanceof HudModule ? ROW : 0) - viewH);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Util.lerp(settingsScroll, settingsScrollTo, 0.3f);

        if (dragging != null) {
            if (!Mouse.isButtonDown(0)) dragging = null;
            else dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW)));
        }

        clip(x0 - 2, top, w + 4, viewH);
        float y = top - settingsScroll;
        for (Setting st : open.settings) {
            R.roundRect(x0, y, w, ROW - 4, 5, CARD);
            Fnt.draw(st.name, x0 + 10, y + 6.5f, 9, TEXT);
            float rx = x0 + w - 10;
            if (st instanceof Setting.Bool) {
                Setting.Bool b = (Setting.Bool) st;
                float a = anim(st, b.value ? 1 : 0);
                R.roundRect(rx - 28, y + 5, 28, 12, 6, R.mix(0xFF343A47, R.ACCENT, a));
                R.circle(rx - 22 + a * 16, y + 11, 4.5f, 0xFFFFFFFF);
            } else if (st instanceof Setting.Num) {
                Setting.Num n = (Setting.Num) st;
                float sw = 110, sx = rx - sw;
                float f = (float) ((n.value - n.min) / (n.max - n.min));
                R.roundRect(sx, y + 9.5f, sw, 3, 1.5f, 0xFF343A47);
                R.roundRect(sx, y + 9.5f, sw * f, 3, 1.5f, R.ACCENT);
                R.circle(sx + sw * f, y + 11, 4.5f, 0xFFFFFFFF);
                String v = n.display();
                Fnt.draw(v, sx - 8 - Fnt.width(v, 8), y + 7, 8, MUTED);
            } else if (st instanceof Setting.Mode) {
                Setting.Mode m = (Setting.Mode) st;
                float bw = 110, bx = rx - bw;
                boolean hov = R.in(mx, my, bx, y + 3, bw, 16);
                R.roundRect(bx, y + 3, bw, 16, 4, hov ? 0xFF303644 : 0xFF262B36);
                Fnt.draw("<", bx + 6, y + 6.5f, 9, MUTED);
                Fnt.draw(">", bx + bw - 11, y + 6.5f, 9, MUTED);
                Fnt.center(Fnt.fit(m.get(), 8, bw - 28), bx + bw / 2f, y + 7, 8, TEXT);
            } else if (st instanceof Setting.Text) {
                Setting.Text ts = (Setting.Text) st;
                float bw = 150, bx = rx - bw;
                boolean focus = editing == ts;
                R.roundRect(bx, y + 3, bw, 16, 4, 0xFF262B36);
                R.roundOutline(bx, y + 3, bw, 16, 4, 1f, focus ? R.ACCENT : 0xFF3A4150);
                String shown = ts.value;
                while (shown.length() > 0 && Fnt.width(shown, 8) > bw - 14) shown = shown.substring(1);
                if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
                if (shown.isEmpty() && !focus) Fnt.draw("Click to type...", bx + 6, y + 7, 8, 0xFF5C6475);
                else Fnt.draw(shown, bx + 6, y + 7, 8, TEXT);
            } else if (st instanceof Setting.Col) {
                Setting.Col c = (Setting.Col) st;
                int n = Setting.Col.PALETTE.length;
                float sx = rx - (n + 1) * 14 + 2;
                for (int i = 0; i <= n; i++) {
                    float cx = sx + i * 14;
                    boolean sel = i == n ? c.chroma : (!c.chroma && c.color == Setting.Col.PALETTE[i]);
                    if (i == n) { R.rainbow(cx + 1, y + 6, 10, 10); }
                    else R.roundRect(cx, y + 5, 12, 12, 3, Setting.Col.PALETTE[i]);
                    R.roundOutline(cx, y + 5, 12, 12, 3, sel ? 2f : 1f, sel ? 0xFFFFFFFF : 0xFF3A4150);
                }
            }
            y += ROW;
        }
        if (open instanceof HudModule) {
            R.roundRect(x0, y, w, ROW - 4, 5, CARD);
            Fnt.draw("Position & size", x0 + 10, y + 6.5f, 9, TEXT);
            boolean hov = R.in(mx, my, x0 + w - 120, y + 3, 110, 16);
            R.roundRect(x0 + w - 120, y + 3, 110, 16, 4, hov ? R.ACCENT : 0xFF262B36);
            Fnt.center("Open HUD Editor", x0 + w - 65, y + 7, 8, TEXT);
        }
        R.endScissor();
    }

    // ---------------------------------------------------------------- input

    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0 && button != 1) return;
        mx = tx(mx); my = ty(my);
        searching = false;
        editing = null;
        // sidebar
        int y = py + 54;
        for (int i = -1; i < Category.values().length; i++) {
            if (R.in(mx, my, px + 8, y, SIDE - 16, 20)) {
                category = i < 0 ? null : Category.values()[i];
                open = null; search = ""; scrollTo = 0; profiles = false;
                return;
            }
            y += 22;
        }
        if (R.in(mx, my, px + 8, py + H - 84, SIDE - 16, 20)) { profiles = !profiles; open = null; profileNote = ""; return; }
        if (R.in(mx, my, px + 8, py + H - 60, SIDE - 16, 20)) { Fnt.minecraft = !Fnt.minecraft; return; }
        if (profiles) { clickProfiles(mx, my); return; }
        if (R.in(mx, my, px + 8, py + H - 34, SIDE - 16, 24)) { mc.displayGuiScreen(new HudEditorScreen()); return; }

        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        if (open == null) {
            if (R.in(mx, my, x0 + w - 120, y0, 120, 18)) { searching = true; return; }
            int top = y0 + 30, viewH = H - 44 - 30;
            if (my < top || my >= top + viewH) return;
            int cw = (w - GAP * 2) / 3;
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
                if (!R.in(mx, my, cx, cy, cw, CARD_H)) continue;
                boolean gear = !m.settings.isEmpty();
                if (gear && (button == 1 || R.in(mx, my, cx + cw - 22, cy + CARD_H - 22, 16, 16))) { open = m; settingsScrollTo = settingsScroll = 0; }
                else if (R.in(mx, my, cx + 6, cy + CARD_H - 22, cw - 12 - (gear ? 20 : 0), 16)) m.toggle();
                return;
            }
            return;
        }

        if (R.in(mx, my, x0, y0, 44, 18)) { open = null; return; }
        if (R.in(mx, my, x0 + w - 64, y0, 64, 18)) { open.toggle(); return; }
        int top = y0 + 42, viewH = H - 56 - 14;
        if (my < top || my >= top + viewH) return;
        float sy = top - settingsScroll;
        float rx = x0 + w - 10;
        for (Setting st : open.settings) {
            if (my >= sy && my < sy + ROW - 4) {
                if (st instanceof Setting.Bool) {
                    if (mx >= rx - 34) ((Setting.Bool) st).value = !((Setting.Bool) st).value;
                } else if (st instanceof Setting.Num) {
                    if (mx >= rx - 116 && mx <= rx + 6) { dragging = (Setting.Num) st; dragX = rx - 110; dragW = 110; }
                } else if (st instanceof Setting.Mode) {
                    if (mx >= rx - 110 && mx <= rx) ((Setting.Mode) st).cycle(button == 1 || mx < rx - 55 ? -1 : 1);
                } else if (st instanceof Setting.Text) {
                    if (mx >= rx - 150) editing = (Setting.Text) st;
                } else if (st instanceof Setting.Col) {
                    Setting.Col c = (Setting.Col) st;
                    int n = Setting.Col.PALETTE.length;
                    float sx = rx - (n + 1) * 14 + 2;
                    int idx = (int) Math.floor((mx - sx) / 14f);
                    if (mx >= sx && idx >= 0 && idx <= n) {
                        if (idx == n) c.chroma = true; else { c.chroma = false; c.color = Setting.Col.PALETTE[idx]; }
                    }
                }
                return;
            }
            sy += ROW;
        }
        if (open instanceof HudModule && my >= sy && my < sy + ROW - 4 && mx >= x0 + w - 120) mc.displayGuiScreen(new HudEditorScreen());
    }

    protected void mouseReleased(int mx, int my, int state) { dragging = null; }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        float step = d > 0 ? -36 : 36;
        if (open == null) scrollTo += step; else settingsScrollTo += step;
    }

    protected void keyTyped(char c, int key) throws IOException {
        if (editing != null) {
            if (key == Keyboard.KEY_RETURN && editing == profileName) { if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } editing = null; }
            else if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) editing = null;
            else if (key == Keyboard.KEY_BACK) { if (!editing.value.isEmpty()) editing.value = editing.value.substring(0, editing.value.length() - 1); }
            else if (key == Keyboard.KEY_V && isCtrlKeyDown()) {
                String clip = ChatAllowedCharacters.filterAllowedCharacters(getClipboardString());
                editing.value = (editing.value + clip).length() > editing.max ? (editing.value + clip).substring(0, editing.max) : editing.value + clip;
            } else if (ChatAllowedCharacters.isAllowedCharacter(c) && editing.value.length() < editing.max) editing.value += c;
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            if (searching) { searching = false; return; }
            if (profiles) { profiles = false; return; }
            if (open != null) { open = null; return; }
            mc.displayGuiScreen(null);
            return;
        }
        if (key == Lumen.MENU_KEY.getKeyCode() && !searching) { mc.displayGuiScreen(null); return; }
        if (open != null || profiles) return;
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            searching = true;
        } else if (ChatAllowedCharacters.isAllowedCharacter(c) && search.length() < 24) {
            search += c;
            searching = true;
            scrollTo = 0;
        }
    }
}
