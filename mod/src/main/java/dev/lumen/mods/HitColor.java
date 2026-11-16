package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class HitColor extends Module {
    public static HitColor INSTANCE;
    private final Setting.Col color = col("Color", 0xFFFF0000);
    private final Setting.Num opacity = num("Opacity %", 30, 5, 80, 5);

    public HitColor() {
        super("Hit Color", "Color of the tint on damaged entities.", Category.VISUAL);
        INSTANCE = this;
    }

    /** Component 0-2 = red, green, blue; 3 = strength. */
    public float component(int i) {
        int c = color.get();
        return i == 0 ? (c >> 16 & 255) / 255f : i == 1 ? (c >> 8 & 255) / 255f : i == 2 ? (c & 255) / 255f : opacity.f() / 100f;
    }
}
