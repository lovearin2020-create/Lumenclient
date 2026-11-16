package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S37PacketStatistics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    @Inject(method = "handleStatistics", at = @At("HEAD"))
    private void lumen$stats(S37PacketStatistics packet, CallbackInfo ci) { Hooks.statsReply(); }
}
