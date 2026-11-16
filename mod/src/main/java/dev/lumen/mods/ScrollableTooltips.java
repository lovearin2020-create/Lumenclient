package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class ScrollableTooltips extends Module {
    public static ScrollableTooltips INSTANCE;
    private final Setting.Num speed = num("Scroll Speed", 10, 2, 30, 1);
    private float sx, sy;
    private long lastDraw;
    private int depth;

    public ScrollableTooltips() {
        super("Scrollable Tooltips", "Scroll long tooltips (SHIFT = sideways).", Category.VISUAL);
        INSTANCE = this;
    }

    public void pre() {
        long now = System.currentTimeMillis();
        if (now - lastDraw > 150) { sx = 0; sy = 0; }
        lastDraw = now;
        depth++;
        GlStateManager.pushMatrix();
        GlStateManager.translate(sx, sy, 0);
    }

    public void post() {
        if (depth <= 0) return;
        depth--;
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        int d = Mouse.getEventDWheel();
        if (d == 0 || System.currentTimeMillis() - lastDraw > 150) return;
        float step = (d > 0 ? 1 : -1) * speed.f();
        if (GuiScreen.isShiftKeyDown()) sx += step; else sy += step;
    }
}
