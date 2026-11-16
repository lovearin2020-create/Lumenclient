package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

public class ChatTweaks extends Module {
    public static ChatTweaks INSTANCE;
    public final Setting.Bool noBackground = bool("Transparent Background", true);
    public final Setting.Num history = num("History Length", 1000, 100, 5000, 100);

    public ChatTweaks() {
        super("Chat Tweaks", "Clear chat background, longer history.", Category.VISUAL);
        INSTANCE = this;
    }
}
