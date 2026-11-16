package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int lumen$history(int original) { return Hooks.chatHistory(original); }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V", ordinal = 0))
    private void lumen$background(int left, int top, int right, int bottom, int color) {
        if (!Hooks.chatNoBackground()) Gui.drawRect(left, top, right, bottom, color);
    }
}
