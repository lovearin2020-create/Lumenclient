package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client-side timer that starts when you throw an ender pearl. Set the length to your server's cooldown. */
public class Cooldowns extends HudModule {
    private final Setting.Num pearl = num("Ender Pearl Cooldown (s)", 16, 1, 30, 1);
    private final Setting.Col color = col("Bar Color", 0xFF38D9F5);
    private long until, length;
    private boolean wasDown;
    private static final ItemStack ICON = new ItemStack(Items.ender_pearl);

    public Cooldowns() { super("Cooldowns", "Ender pearl cooldown timer.", CENTER, 20, CENTER, 24); }

    public float width() { return 62; }
    public float height() { return 20; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        boolean down = mc.currentScreen == null && mc.gameSettings.keyBindUseItem.isKeyDown();
        ItemStack held = mc.thePlayer.getHeldItem();
        if (down && !wasDown && held != null && held.getItem() == Items.ender_pearl && System.currentTimeMillis() >= until) {
            length = (long) (pearl.value * 1000);
            until = System.currentTimeMillis() + length;
        }
        wasDown = down;
    }

    public void draw(boolean editing) {
        long left = until - System.currentTimeMillis();
        if (left <= 0 && !editing) return;
        float f = left <= 0 ? 0.6f : left / (float) Math.max(1, length);
        R.roundRect(0, 0, 62, 20, 3, 0x70000000);
        ArmorStatus.drawItem(ICON, 2, 2);
        R.mcText(String.format("%.1fs", left <= 0 ? pearl.value * 0.6 : left / 1000.0), 22, 3, 0xFFFFFFFF, true);
        R.rect(22, 14, 36, 2, 0x50FFFFFF);
        R.rect(22, 14, 36 * f, 2, color.get());
    }
}
