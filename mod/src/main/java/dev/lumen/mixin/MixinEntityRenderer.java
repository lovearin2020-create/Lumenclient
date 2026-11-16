package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void lumen$hurtCam(float partialTicks, CallbackInfo ci) { if (Hooks.noHurtCam()) ci.cancel(); }
}
