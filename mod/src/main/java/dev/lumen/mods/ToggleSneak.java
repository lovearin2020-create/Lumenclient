package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ToggleSneak extends TextHud {
    private final Setting.Bool showText = bool("Show Status Text", true);
    private boolean toggled;

    public ToggleSneak() { super("Toggle Sneak", "Tap sneak to stay sneaking.", START, 4, END, 22); }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        int code = mc.gameSettings.keyBindSneak.getKeyCode();
        if (code <= 0 || mc.currentScreen != null) return;
        if (Keyboard.getEventKey() == code && Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) toggled = !toggled;
        if (Keyboard.getEventKey() == code) KeyBinding.setKeyBindState(code, toggled || Keyboard.getEventKeyState());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START || mc.thePlayer == null) { if (mc.thePlayer == null) toggled = false; return; }
        if (toggled && mc.currentScreen == null) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
    }

    protected void onDisable() {
        if (toggled && mc.gameSettings != null) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        toggled = false;
    }

    protected int minWidth() { return 0; }

    protected String text(boolean editing) {
        if (editing) return "[Sneaking (Toggled)]";
        return toggled && showText.on() ? "[Sneaking (Toggled)]" : null;
    }
}
