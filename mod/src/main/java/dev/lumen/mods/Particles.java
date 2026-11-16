package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Particles extends Module {
    private final Setting.Num multiplier = num("Hit Particle Multiplier", 2, 1, 10, 1);
    private final Setting.Bool alwaysCrit = bool("Always Show Crit Particles", false);
    private final Setting.Bool alwaysSharp = bool("Always Show Sharpness Particles", true);

    public Particles() { super("Particles", "More crit and sharpness particles on hit.", Category.VISUAL); }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer != mc.thePlayer || e.target == null || mc.effectRenderer == null) return;
        boolean crit = mc.thePlayer.fallDistance > 0 && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && mc.thePlayer.ridingEntity == null;
        int extra = multiplier.i() - 1;
        if (crit || alwaysCrit.on()) for (int i = 0; i < (crit ? extra : extra + 1); i++) mc.effectRenderer.emitParticleAtEntity(e.target, EnumParticleTypes.CRIT);
        if (alwaysSharp.on()) for (int i = 0; i < extra + 1; i++) mc.effectRenderer.emitParticleAtEntity(e.target, EnumParticleTypes.CRIT_MAGIC);
    }
}
