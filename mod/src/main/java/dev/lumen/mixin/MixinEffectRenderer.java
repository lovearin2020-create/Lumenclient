package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public abstract class MixinEffectRenderer {
    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void lumen$limit(EntityFX fx, CallbackInfo ci) { if (Hooks.dropParticle()) ci.cancel(); }
}
