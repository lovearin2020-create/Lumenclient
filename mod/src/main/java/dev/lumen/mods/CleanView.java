package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;

public class CleanView extends Module {
    public static CleanView INSTANCE;

    public CleanView() {
        super("Clean View", "Hides your own potion particles in first person.", Category.VISUAL);
        INSTANCE = this;
    }
}
