package dev.lumen.mods;

import dev.lumen.core.TextHud;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReachDisplay extends TextHud {
    private double reach;
    private long last;

    public ReachDisplay() { super("Reach Display", "Distance of your last hit.", CENTER, -28, CENTER, 42); }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer != mc.thePlayer) return;
        MovingObjectPosition m = mc.objectMouseOver;
        if (m != null && m.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && m.hitVec != null) {
            reach = m.hitVec.distanceTo(mc.thePlayer.getPositionEyes(1.0F));
            last = System.currentTimeMillis();
        }
    }

    protected String text(boolean editing) {
        if (System.currentTimeMillis() - last > 3000) return editing ? "0.00 blocks" : null;
        return String.format("%.2f blocks", reach);
    }
}
