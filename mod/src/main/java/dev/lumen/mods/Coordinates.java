package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class Coordinates extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool direction = bool("Direction", true);
    private final Setting.Bool biome = bool("Biome", false);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private float w = 70;

    public Coordinates() { super("Coordinates", "Position and facing direction.", START, 4, START, 40); }

    public float width() { return w; }
    public float height() { return 6 + 30 + (biome.on() ? 10 : 0); }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        int x = MathHelper.floor_double(mc.thePlayer.posX), y = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY), z = MathHelper.floor_double(mc.thePlayer.posZ);
        String[] lines = {"X: " + x, "Y: " + y, "Z: " + z};
        String b = null;
        if (biome.on() && mc.theWorld != null) {
            try { b = mc.theWorld.getBiomeGenForCoords(new BlockPos(x, y, z)).biomeName; } catch (Throwable t) { b = "Unknown"; }
        }
        float max = 58;
        for (String s : lines) max = Math.max(max, mc.fontRendererObj.getStringWidth(s) + 26);
        if (b != null) max = Math.max(max, mc.fontRendererObj.getStringWidth(b) + 10);
        w = max;
        if (background.on()) R.roundRect(0, 0, w, height(), 3, 0x70000000);
        for (int i = 0; i < 3; i++) R.mcText(lines[i], 5, 4 + i * 10, color.get(), true);
        if (b != null) R.mcText(b, 5, 34, 0xFFB8C0D0, true);
        if (direction.on()) {
            String d = DIRS[MathHelper.floor_double(mc.thePlayer.rotationYaw * 8.0F / 360.0F + 0.5D) & 7];
            R.mcText(d, w - 5 - mc.fontRendererObj.getStringWidth(d), 14, R.ACCENT, true);
        }
    }
}
