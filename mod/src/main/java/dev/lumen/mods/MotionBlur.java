package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class MotionBlur extends Module {
    public static MotionBlur INSTANCE;
    public final Setting.Num amount = num("Amount", 4, 1, 8, 1);

    public MotionBlur() {
        super("Motion Blur", "Blends frames together. Needs OptiFine Fast Render off.", Category.VISUAL);
        INSTANCE = this;
    }
}
