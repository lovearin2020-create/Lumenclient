package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {
    @Shadow public double entityX;
    @Shadow public double entityY;
    @Shadow public double entityZ;

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void lumen$cull(TileEntity te, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (te != null && destroyStage < 0 && Hooks.cullTile(te.getDistanceSq(entityX, entityY, entityZ))) ci.cancel();
    }
}
