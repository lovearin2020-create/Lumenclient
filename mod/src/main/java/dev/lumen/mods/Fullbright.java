package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;

public class Fullbright extends Module {
    private float saved = 1f;

    public Fullbright() { super("Fullbright", "Maximum brightness.", Category.VISUAL); }

    protected void onEnable() {
        saved = Math.min(1f, mc.gameSettings.gammaSetting);
        mc.gameSettings.gammaSetting = 100f;
    }

    protected void onDisable() { mc.gameSettings.gammaSetting = saved; }
}
