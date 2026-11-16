package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {
    @Inject(method = "doRenderEntity", at = @At("HEAD"), cancellable = true)
    private void lumen$cull(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.cullEntity(entity, x, y, z)) cir.setReturnValue(false);
    }
}
