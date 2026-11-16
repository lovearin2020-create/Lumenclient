package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;

import java.util.List;

public class PackDisplay extends HudModule {
    private final Setting.Bool icon = bool("Pack Icon", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private float w = 100;

    public PackDisplay() { super("Pack Display", "Shows your active resource pack.", END, 4, END, 100); }

    public float width() { return w; }
    public float height() { return 24; }

    public void draw(boolean editing) {
        List<ResourcePackRepository.Entry> list = mc.getResourcePackRepository().getRepositoryEntries();
        ResourcePackRepository.Entry top = list.isEmpty() ? null : list.get(list.size() - 1);
        String name = top == null ? "Default" : Util.strip(top.getResourcePackName()).replace(".zip", "");
        if (name.length() > 28) name = name.substring(0, 27) + "...";
        int tx = icon.on() && top != null ? 26 : 6;
        w = tx + mc.fontRendererObj.getStringWidth(name) + 6;
        R.roundRect(0, 0, w, 24, 3, 0x70000000);
        if (icon.on() && top != null) {
            try {
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                top.bindTexturePackIcon(mc.getTextureManager());
                Gui.drawModalRectWithCustomSizedTexture(3, 3, 0, 0, 18, 18, 18, 18);
            } catch (Throwable ignored) {}
        }
        R.mcText(name, tx, 8, color.get(), true);
    }
}
