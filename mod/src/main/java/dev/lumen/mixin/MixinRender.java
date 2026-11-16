package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class MixinRender {
    private boolean lumen$renaming;

    @Shadow protected void renderLivingLabel(Entity entity, String str, double x, double y, double z, int maxDistance) {}

    /** Own Nametag: draw your custom text instead of your name. Only the label over your own head changes. */
    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    private void lumen$ownName(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (lumen$renaming) return;
        String custom = Hooks.ownNameText(entity);
        if (custom == null) return;
        lumen$renaming = true;
        try { this.renderLivingLabel(entity, custom, x, y, z, maxDistance); } finally { lumen$renaming = false; }
        ci.cancel();
    }
}
