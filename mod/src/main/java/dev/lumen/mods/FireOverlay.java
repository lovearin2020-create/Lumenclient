package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class FireOverlay extends Module {
    public static FireOverlay INSTANCE;
    public final Setting.Num lower = num("Lower By", 0.25, 0, 0.5, 0.05);

    public FireOverlay() {
        super("Low Fire", "Lowers the fire overlay.", Category.VISUAL);
        INSTANCE = this;
    }
}
