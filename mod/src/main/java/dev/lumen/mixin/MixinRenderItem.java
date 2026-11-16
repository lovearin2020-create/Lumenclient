package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {
    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true)
    private void lumen$glint(IBakedModel model, CallbackInfo ci) { if (Hooks.noGlint()) ci.cancel(); }
}
