package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class MixinScoreboard {
    @Inject(method = "removeTeam", at = @At("HEAD"), cancellable = true)
    private void lumen$nullTeam(ScorePlayerTeam team, CallbackInfo ci) { if (Hooks.ignoreNullScoreboardRemoval(team)) ci.cancel(); }

    @Inject(method = "removeObjective", at = @At("HEAD"), cancellable = true)
    private void lumen$nullObjective(ScoreObjective objective, CallbackInfo ci) { if (Hooks.ignoreNullScoreboardRemoval(objective)) ci.cancel(); }
}
