package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    @Shadow private IChatComponent header;
    @Shadow private IChatComponent footer;
    private IChatComponent lumen$header, lumen$footer;
    private boolean lumen$hidden;

    @Inject(method = "drawPing", at = @At("HEAD"), cancellable = true)
    private void lumen$ping(int columnWidth, int x, int y, NetworkPlayerInfo info, CallbackInfo ci) {
        if (Hooks.tabPing(columnWidth, x, y, info)) ci.cancel();
    }

    @Inject(method = "renderPlayerlist", at = @At("HEAD"))
    private void lumen$hide(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        lumen$header = header; lumen$footer = footer; lumen$hidden = true;
        if (Hooks.tabHideHeader()) header = null;
        if (Hooks.tabHideFooter()) footer = null;
    }

    @Inject(method = "renderPlayerlist", at = @At("RETURN"))
    private void lumen$restore(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        if (!lumen$hidden) return;
        lumen$hidden = false;
        header = lumen$header; footer = lumen$footer;
    }
}
