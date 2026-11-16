package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class Titles extends Module {
    public static Titles INSTANCE;
    public final Setting.Bool hide = bool("Hide Titles", false);
    public final Setting.Num scale = num("Scale", 0.7, 0.3, 1.5, 0.05);

    public Titles() {
        super("Titles", "Resize or hide big on-screen titles.", Category.HUD);
        INSTANCE = this;
    }
}
