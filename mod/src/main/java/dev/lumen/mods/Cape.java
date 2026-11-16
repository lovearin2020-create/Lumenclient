package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class Cape extends Module {
    public static Cape INSTANCE;
    public final Setting.Mode style = mode("Design", "Aurora", "Aurora", "Galaxy", "Sakura", "Ember", "Royal", "Synthwave", "Frost", "Lumen", "Midnight", "Sunset", "Ocean", "Blossom", "Mono", "Custom Colors", "Custom PNG");
    public final Setting.Col primary = col("Custom Color 1", 0xFF4C8DFF);
    public final Setting.Col secondary = col("Custom Color 2", 0xFFA06BFF);
    public final Setting.Bool emblem = bool("Emblem (gradient designs)", true);

    public Cape() {
        super("Cape", "Client-side cape. Custom PNG: config/lumen/cape.png", Category.COSMETICS);
        INSTANCE = this;
    }
}
