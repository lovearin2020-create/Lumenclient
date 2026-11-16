package dev.lumen.mods;

import dev.lumen.core.TextHud;

public class Saturation extends TextHud {
    public Saturation() { super("Saturation", "Your hidden food saturation.", CENTER, 100, END, 26); }

    protected String text(boolean editing) {
        if (mc.thePlayer == null) return null;
        return String.format("Sat: %.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
    }
}
