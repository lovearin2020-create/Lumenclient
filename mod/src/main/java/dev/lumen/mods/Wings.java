package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class Wings extends Module {
    public static Wings INSTANCE;
    public final Setting.Mode style = mode("Style", "Ender Dragon", "Ender Dragon", "Crystal");
    public final Setting.Bool tint = bool("Tint Dragon Wings", false);
    public final Setting.Col color = col("Color", 0xFF4C8DFF);
    public final Setting.Num size = num("Size", 1.0, 0.5, 2.0, 0.1);
    public final Setting.Num speed = num("Flap Speed", 1.0, 0.2, 3.0, 0.1);
    public final Setting.Num opacity = num("Crystal Opacity", 60, 20, 100, 5);

    public Wings() {
        super("Wings", "Client-side wings (third person).", Category.COSMETICS);
        INSTANCE = this;
    }
}
