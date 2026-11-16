package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LayerArmorBase.class)
public abstract class MixinLayerArmorBase {
    @Inject(method = "shouldCombineTextures", at = @At("HEAD"), cancellable = true)
    private void lumen$tint(CallbackInfoReturnable<Boolean> cir) { if (Hooks.armorTint()) cir.setReturnValue(true); }
}
