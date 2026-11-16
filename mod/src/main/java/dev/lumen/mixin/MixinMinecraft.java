package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow private void runGameLoop() throws IOException {}

    /** Crash Patcher: a crash inside one frame no longer has to end the whole game loop. */
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runGameLoop()V"))
    private void lumen$guardedLoop(Minecraft self) throws IOException {
        try {
            this.runGameLoop();
        } catch (Throwable t) {
            if (!Hooks.handleCrash(t)) throw t;
        }
    }

    /** glGetError forces the driver to synchronise three times per frame; skipping it is a free speed-up. */
    @Inject(method = "checkGLError", at = @At("HEAD"), cancellable = true)
    private void lumen$skipGlError(String message, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (Hooks.skipGlErrorChecks()) ci.cancel();
    }

    @Inject(method = "getLimitFramerate", at = @At("RETURN"), cancellable = true)
    private void lumen$frameLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        int limit = Hooks.frameLimit(original);
        if (limit != original) cir.setReturnValue(limit);
    }
}
