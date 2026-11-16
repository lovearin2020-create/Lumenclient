package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    /** Clean View: skip spawning your own potion swirls while in first person. */
    @Redirect(method = "updatePotionEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void lumen$potionParticles(World world, EnumParticleTypes type, double x, double y, double z, double dx, double dy, double dz, int[] args) {
        if (!Hooks.hideOwnPotionParticles(this)) world.spawnParticle(type, x, y, z, dx, dy, dz, args);
    }
}
