package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.util.MathHelper;

public class DirectionHud extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col color = col("Marker Color", 0xFF4C8DFF);
    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionHud() { super("Direction HUD", "Compass bar.", CENTER, -90, START, 4); }

    public float width() { return 180; }
    public float height() { return 20; }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        if (background.on()) R.roundRect(0, 0, 180, 20, 3, 0x70000000);
        float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        float per = 1.2f; // pixels per degree
        for (int deg = -180; deg < 180; deg += 15) {
            float off = MathHelper.wrapAngleTo180_float(deg - yaw) * per;
            if (Math.abs(off) > 84) continue;
            float a = Math.min(1f, (84 - Math.abs(off)) / 20f);
            float x = 90 + off;
            if (deg % 45 == 0) {
                String n = NAMES[((deg + 360) % 360) / 45];
                boolean main = n.length() == 1;
                int col = R.alpha(main ? 0xFFFFFFFF : 0xFFB8C0D0, a);
                if ((col >>> 24) > 8) R.mcText(n, Math.round(x - mc.fontRendererObj.getStringWidth(n) / 2f), 3, col, false);
                R.rect(x - 0.5f, 14, 1, 4, R.alpha(0xFFFFFFFF, a));
            } else {
                R.rect(x - 0.5f, 15, 1, 2, R.alpha(0x99FFFFFF, a));
            }
        }
        R.rect(89.5f, 1, 1, 18, color.get());
    }
}
