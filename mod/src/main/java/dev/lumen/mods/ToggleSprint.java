package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ToggleSprint extends TextHud {
    private final Setting.Bool showText = bool("Show Status Text", true);

    public ToggleSprint() { super("Toggle Sprint", "Always sprint.", START, 4, END, 4); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.currentScreen != null) return;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
    }

    protected void onDisable() {
        if (mc.gameSettings != null) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
    }

    protected int minWidth() { return 0; }

    protected String text(boolean editing) {
        if (!showText.on() && !editing) return null;
        return "[Sprinting (Toggled)]";
    }
}
