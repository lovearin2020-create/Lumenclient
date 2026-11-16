package dev.lumen.mods;

import dev.lumen.core.TextHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ComboCounter extends TextHud {
    private int combo, lastHurt;
    private Entity target;
    private long lastAttack, lastHit;

    public ComboCounter() { super("Combo Counter", "Hits landed in a row.", CENTER, -28, CENTER, 24); }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer != mc.thePlayer) return;
        target = e.target;
        lastAttack = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        long now = System.currentTimeMillis();
        if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) combo = 0;
        if (target instanceof EntityLivingBase) {
            int h = ((EntityLivingBase) target).hurtTime;
            if (h > lastHurt && now - lastAttack < 600) { combo++; lastHit = now; }
            lastHurt = h;
        }
        if (combo > 0 && now - lastHit > 2500) combo = 0;
    }

    protected String text(boolean editing) {
        if (combo == 0) return editing ? "No Combo" : null;
        return combo + " Combo";
    }
}
