package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FovTweaks extends Module {
    private final Setting.Num strength = num("Dynamic FOV Strength %", 0, 0, 100, 5);

    public FovTweaks() { super("Static FOV", "Limits sprint, speed and bow FOV changes.", Category.VISUAL); }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        e.newfov = 1f + (e.newfov - 1f) * (strength.f() / 100f);
    }
}
