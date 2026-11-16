package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

public class PotCounter extends TextHud {
    private final Setting.Mode item = mode("Item", "Splash Potions", "Splash Potions", "Soup", "Golden Apples");

    public PotCounter() { super("Pot Counter", "Healing items left.", CENTER, -28, CENTER, 60); }

    protected String text(boolean editing) {
        int n = 0;
        if (mc.thePlayer != null) {
            for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
                if (s == null) continue;
                if (item.is("Splash Potions")) { if (s.getItem() == Items.potionitem && ItemPotion.isSplash(s.getMetadata())) n += s.stackSize; }
                else if (item.is("Soup")) { if (s.getItem() == Items.mushroom_stew) n += s.stackSize; }
                else if (s.getItem() == Items.golden_apple) n += s.stackSize;
            }
        }
        return n + (item.is("Splash Potions") ? " Pots" : item.is("Soup") ? " Soups" : " Gapples");
    }
}
