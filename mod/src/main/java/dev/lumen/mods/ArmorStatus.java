package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatus extends HudModule {
    private final Setting.Bool held = bool("Held Item", true);
    private final Setting.Mode show = mode("Durability", "Remaining", "Remaining", "Percent", "Hidden");
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 4;

    public ArmorStatus() { super("Armor Status", "Armor and durability.", END, 4, END, 4); }

    public float width() { return show.is("Hidden") ? 18 : 48; }
    public float height() { return Math.max(1, rows) * 17; }

    public void draw(boolean editing) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        if (mc.thePlayer != null) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = mc.thePlayer.inventory.armorInventory[i];
                if (s != null) list.add(s);
            }
            if (held.on() && mc.thePlayer.getHeldItem() != null) list.add(mc.thePlayer.getHeldItem());
        }
        if (list.isEmpty() && editing) {
            list.add(new ItemStack(Items.diamond_helmet)); list.add(new ItemStack(Items.diamond_chestplate));
            list.add(new ItemStack(Items.diamond_leggings)); list.add(new ItemStack(Items.diamond_boots));
        }
        rows = list.size();
        int y = 0;
        for (ItemStack s : list) {
            drawItem(s, 0, y);
            if (!show.is("Hidden")) {
                String t = null;
                if (s.isItemStackDamageable()) {
                    int left = s.getMaxDamage() - s.getItemDamage();
                    t = show.is("Percent") ? (left * 100 / Math.max(1, s.getMaxDamage())) + "%" : String.valueOf(left);
                } else if (s.stackSize > 1) t = "x" + s.stackSize;
                if (t != null) R.mcText(t, 19, y + 4, color.get(), true);
            }
            y += 17;
        }
    }

    static void drawItem(ItemStack s, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        R.reset();
    }
}
