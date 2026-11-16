package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Inject(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V", at = @At("HEAD"), remap = false)
    private void lumen$tooltipPre(List<String> lines, int x, int y, FontRenderer font, CallbackInfo ci) { Hooks.tooltipPre(); }

    @Inject(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V", at = @At("RETURN"), remap = false)
    private void lumen$tooltipPost(List<String> lines, int x, int y, FontRenderer font, CallbackInfo ci) { Hooks.tooltipPost(); }
}
