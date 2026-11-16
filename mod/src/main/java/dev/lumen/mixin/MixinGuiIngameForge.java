package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForge {
    private boolean lumen$scaled;

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true, remap = false)
    private void lumen$titlePre(int width, int height, float partialTicks, CallbackInfo ci) {
        if (Hooks.hideTitles()) { ci.cancel(); return; }
        float s = Hooks.titleScale();
        if (s == 1f) return;
        lumen$scaled = true;
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(s, s, 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);
    }

    @Inject(method = "renderTitle", at = @At("RETURN"), remap = false)
    private void lumen$titlePost(int width, int height, float partialTicks, CallbackInfo ci) {
        if (!lumen$scaled) return;
        lumen$scaled = false;
        GlStateManager.popMatrix();
    }
}
