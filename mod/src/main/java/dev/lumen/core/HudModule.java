package dev.lumen.core;

import dev.lumen.util.R;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/** A module that draws a movable, scalable element on the in-game HUD. */
public abstract class HudModule extends Module {
    public static final int START = 0, CENTER = 1, END = 2;

    /** Which screen edge (or the centre) the element is attached to, and its pixel offset from it. */
    public int ax, ay;
    public float ox, oy;
    public float scale = 1f;

    protected HudModule(String name, String description, int ax, float ox, int ay, float oy) {
        super(name, description, Category.HUD);
        this.ax = ax; this.ox = ox; this.ay = ay; this.oy = oy;
    }

    /** Unscaled size of the element. */
    public abstract float width();
    public abstract float height();

    /** Draw at the origin. {@code editing} is true inside the HUD editor (show placeholder data). */
    public abstract void draw(boolean editing);

    private static float resolve(int anchor, float off, float screen, float size) {
        float v = anchor == START ? off : anchor == CENTER ? screen / 2f + off : screen - off - size;
        return Math.max(0, Math.min(Math.max(0, screen - size), v));
    }

    public float px(ScaledResolution sr) { return resolve(ax, ox, sr.getScaledWidth(), width() * scale); }
    public float py(ScaledResolution sr) { return resolve(ay, oy, sr.getScaledHeight(), height() * scale); }

    /** Moves the element so its top-left corner is at (x, y), re-attaching it to the nearest edge. */
    public void moveTo(ScaledResolution sr, float x, float y) {
        float sw = sr.getScaledWidth(), sh = sr.getScaledHeight(), w = width() * scale, h = height() * scale;
        float cx = x + w / 2f, cy = y + h / 2f;
        ax = cx < sw / 3f ? START : cx > sw * 2f / 3f ? END : CENTER;
        ay = cy < sh / 3f ? START : cy > sh * 2f / 3f ? END : CENTER;
        ox = ax == START ? x : ax == CENTER ? x - sw / 2f : sw - x - w;
        oy = ay == START ? y : ay == CENTER ? y - sh / 2f : sh - y - h;
    }

    public void render(ScaledResolution sr, boolean editing) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(Math.round(px(sr)), Math.round(py(sr)), 0);
        GlStateManager.scale(scale, scale, 1);
        try { draw(editing); } catch (Throwable ignored) {}
        GlStateManager.popMatrix();
        R.reset();
    }
}
