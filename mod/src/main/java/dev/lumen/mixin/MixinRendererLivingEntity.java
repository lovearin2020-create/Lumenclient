package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void lumen$ownName(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.ownNametag(entity)) cir.setReturnValue(true);
    }

    // Hit Color: the first four values written in setBrightness are the red damage tint (r, g, b, strength).
    private static final String PUT = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;";

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = PUT, ordinal = 0, remap = false))
    private float lumen$hitR(float v) { return Hooks.hitColor(0, v); }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = PUT, ordinal = 1, remap = false))
    private float lumen$hitG(float v) { return Hooks.hitColor(1, v); }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = PUT, ordinal = 2, remap = false))
    private float lumen$hitB(float v) { return Hooks.hitColor(2, v); }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = PUT, ordinal = 3, remap = false))
    private float lumen$hitA(float v) { return Hooks.hitColor(3, v); }
}
