package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;

public class Nametags extends Module {
    public static Nametags INSTANCE;

    private final dev.lumen.core.Setting.Text custom = text("Custom Text (& = color code)", "", 32);

    /** Replacement for your own nametag, or null to keep the real one. Chat and tab are not affected. */
    public String replacement() {
        String s = custom.value.trim();
        if (s.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) b.append('\u00a7'); else b.append(c);
        }
        return b.toString();
    }

    public Nametags() {
        super("Own Nametag", "Your nametag in third person, with optional custom text.", Category.VISUAL);
        INSTANCE = this;
    }
}
