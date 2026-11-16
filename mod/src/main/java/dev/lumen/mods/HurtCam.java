package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;

public class HurtCam extends Module {
    public static HurtCam INSTANCE;

    public HurtCam() {
        super("No Hurt Cam", "No camera shake on damage.", Category.VISUAL);
        INSTANCE = this;
    }
}
