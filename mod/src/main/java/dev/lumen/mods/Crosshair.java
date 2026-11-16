package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Crosshair extends Module {
    private final Setting.Num length = num("Length", 5, 1, 15, 1);
    private final Setting.Num gap = num("Gap", 2, 0, 10, 1);
    private final Setting.Num thickness = num("Thickness", 1, 1, 4, 1);
    private final Setting.Bool dot = bool("Center Dot", false);
    private final Setting.Bool outline = bool("Outline", true);
    private final Setting.Bool hideF5 = bool("Hide In Third Person", true);
    private final Setting.Col color = col("Color", 0xFFFFFFFF);

    public Crosshair() { super("Crosshair", "Custom crosshair.", Category.VISUAL); }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        e.setCanceled(true);
        if (hideF5.on() && mc.gameSettings.thirdPersonView != 0) return;
        float cx = e.resolution.getScaledWidth() / 2f, cy = e.resolution.getScaledHeight() / 2f;
        float l = length.f(), g = gap.f(), t = thickness.f(), h = t / 2f;
        int c = color.get();
        if (outline.on()) {
            int o = 0xC0000000;
            bar(cx - g - l, cy - h, l, t, o, 0.5f); bar(cx + g, cy - h, l, t, o, 0.5f);
            bar(cx - h, cy - g - l, t, l, o, 0.5f); bar(cx - h, cy + g, t, l, o, 0.5f);
            if (dot.on()) bar(cx - h, cy - h, t, t, o, 0.5f);
        }
        bar(cx - g - l, cy - h, l, t, c, 0); bar(cx + g, cy - h, l, t, c, 0);
        bar(cx - h, cy - g - l, t, l, c, 0); bar(cx - h, cy + g, t, l, c, 0);
        if (dot.on()) bar(cx - h, cy - h, t, t, c, 0);
        R.reset();
    }

    private static void bar(float x, float y, float w, float h, int c, float grow) {
        R.rect(x - grow, y - grow, w + grow * 2, h + grow * 2, c);
    }
}
