package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;

public class TabEditor extends Module {
    public static TabEditor INSTANCE;
    private final Setting.Bool numbers = bool("Ping As Numbers", true);
    public final Setting.Bool hideHeader = bool("Hide Header", false);
    public final Setting.Bool hideFooter = bool("Hide Footer", false);

    public TabEditor() {
        super("Tab Editor", "Ping numbers and cleaner tab list.", Category.HUD);
        INSTANCE = this;
    }

    /** @return true if the vanilla ping bars should be skipped. */
    public boolean drawPing(int columnWidth, int x, int y, NetworkPlayerInfo info) {
        if (!numbers.on() || info == null) return false;
        int ping = info.getResponseTime();
        String s = String.valueOf(ping);
        int color = ping < 0 ? 0xFFAAAAAA : ping < 80 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : ping < 250 ? 0xFFFFAA00 : 0xFFFF5555;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + columnWidth - 1 - mc.fontRendererObj.getStringWidth(s) * 0.5f, y + 2, 0);
        GlStateManager.scale(0.5f, 0.5f, 1);
        mc.fontRendererObj.drawStringWithShadow(s, 0, 0, color);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
        return true;
    }
}
