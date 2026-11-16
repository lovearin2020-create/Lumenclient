package dev.lumen.core;

import dev.lumen.util.R;

/** Lunar-style single line text box, e.g. "120 FPS". */
public abstract class TextHud extends HudModule {
    protected final Setting.Bool background = bool("Background", true);
    protected final Setting.Bool shadow = bool("Text Shadow", true);
    protected final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    protected TextHud(String name, String description, int ax, float ox, int ay, float oy) { super(name, description, ax, ox, ay, oy); }

    /** Text to show, or null to hide. */
    protected abstract String text(boolean editing);
    protected int minWidth() { return 56; }

    private String current = "";

    public float width() { return background.on() ? Math.max(minWidth(), mc.fontRendererObj.getStringWidth(current) + 10) : mc.fontRendererObj.getStringWidth(current); }
    public float height() { return background.on() ? 16 : 9; }

    public void draw(boolean editing) {
        String s = text(editing);
        if (s == null) { current = ""; return; }
        current = s;
        float w = width();
        if (background.on()) {
            R.roundRect(0, 0, w, 16, 3, 0x70000000);
            R.mcText(s, Math.round((w - mc.fontRendererObj.getStringWidth(s)) / 2f), 4, color.get(), shadow.on());
        } else {
            R.mcText(s, 0, 0, color.get(), shadow.on());
        }
    }
}
