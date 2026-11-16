package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TimeChanger extends Module {
    private final Setting.Mode preset = mode("Time", "Day", "Day", "Sunset", "Night", "Custom");
    private final Setting.Num custom = num("Custom Time", 6000, 0, 24000, 500);

    public TimeChanger() { super("Time Changer", "Client-side time of day.", Category.VISUAL); }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.theWorld == null) return;
        long t = preset.is("Day") ? 6000 : preset.is("Sunset") ? 12500 : preset.is("Night") ? 18000 : (long) custom.value;
        mc.theWorld.setWorldTime(t);
    }
}
