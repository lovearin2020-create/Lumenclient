package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PotionEffects extends HudModule {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private final Setting.Bool names = bool("Effect Names", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 1;

    public PotionEffects() { super("Potion Effects", "Active effects and durations.", END, 4, START, 4); }

    public float width() { return names.on() ? 100 : 50; }
    public float height() { return Math.max(1, rows) * 21; }

    public void draw(boolean editing) {
        List<PotionEffect> list = new ArrayList<PotionEffect>();
        if (mc.thePlayer != null) list.addAll(mc.thePlayer.getActivePotionEffects());
        if (list.isEmpty() && editing) list.add(new PotionEffect(Potion.moveSpeed.id, 1800, 1));
        rows = list.size();
        int y = 0;
        for (PotionEffect e : list) {
            Potion p = Potion.potionTypes[e.getPotionID()];
            if (p == null) continue;
            if (p.hasStatusIcon()) {
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                mc.getTextureManager().bindTexture(INVENTORY);
                int idx = p.getStatusIconIndex();
                mc.ingameGUI.drawTexturedModalRect(0, y + 1, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
            }
            String time = Potion.getDurationString(e);
            if (names.on()) {
                String n = I18n.format(p.getName());
                if (e.getAmplifier() > 0 && e.getAmplifier() < 10) n += " " + (e.getAmplifier() + 1);
                R.mcText(n, 21, y + 2, color.get(), true);
                R.mcText(time, 21, y + 11, 0xFFB8C0D0, true);
            } else {
                R.mcText(time, 21, y + 6, color.get(), true);
            }
            y += 21;
        }
    }
}
