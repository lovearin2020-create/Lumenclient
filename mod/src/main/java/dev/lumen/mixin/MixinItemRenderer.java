package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void lumen$firstPerson(float partialTicks, CallbackInfo ci) { Hooks.firstPersonStart(partialTicks); }

    @ModifyVariable(method = "transformFirstPersonItem", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float lumen$swing(float swingProgress) { return Hooks.swingProgress(swingProgress); }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"))
    private void lumen$firePre(float partialTicks, CallbackInfo ci) { Hooks.firePre(); }

    @Inject(method = "renderFireInFirstPerson", at = @At("RETURN"))
    private void lumen$firePost(float partialTicks, CallbackInfo ci) { Hooks.firePost(); }
}
