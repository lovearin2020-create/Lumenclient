package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BossBarMod extends Module {
    public BossBarMod() { super("Hide Boss Bar", "Hides the boss health bar.", Category.HUD); }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre e) {
        if (e.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) e.setCanceled(true);
    }
}
