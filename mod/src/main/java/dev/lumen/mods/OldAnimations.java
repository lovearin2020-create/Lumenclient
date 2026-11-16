package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.item.EnumAction;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class OldAnimations extends Module {
    public static OldAnimations INSTANCE;
    private final Setting.Bool blockHit = bool("Block Hitting", true);
    private final Setting.Bool eating = bool("Eating / Drinking Swing", true);
    private final Setting.Bool bow = bool("Bow Swing", true);
    private final Setting.Bool punching = bool("Punch While Using Item", true);
    public final Setting.Bool armorTint = bool("Red Armor On Damage", true);
    public static float partialTicks;

    public OldAnimations() {
        super("1.7 Animations", "1.7 block hitting and item swings.", Category.VISUAL);
        INSTANCE = this;
    }

    /** Vanilla 1.8 passes 0 here while an item is in use; 1.7 used the real swing progress. */
    public float swingProgress(float original) {
        if (original != 0f || mc.thePlayer == null || mc.thePlayer.getItemInUseCount() <= 0 || mc.thePlayer.getItemInUse() == null) return original;
        EnumAction a = mc.thePlayer.getItemInUse().getItemUseAction();
        boolean apply = a == EnumAction.BLOCK ? blockHit.on() : (a == EnumAction.EAT || a == EnumAction.DRINK) ? eating.on() : a == EnumAction.BOW && bow.on();
        return apply ? mc.thePlayer.getSwingProgress(partialTicks) : original;
    }

    /** Purely visual swing while using an item and holding attack on a block. No packet is sent. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !punching.on() || mc.thePlayer == null || mc.currentScreen != null) return;
        if (!mc.gameSettings.keyBindAttack.isKeyDown() || !mc.thePlayer.isUsingItem()) return;
        MovingObjectPosition m = mc.objectMouseOver;
        if (m == null || m.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= 3 || mc.thePlayer.swingProgressInt < 0) {
            mc.thePlayer.swingProgressInt = -1;
            mc.thePlayer.isSwingInProgress = true;
        }
    }
}
