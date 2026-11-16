package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.Display;

import java.util.Random;

/** Frame-rate oriented tweaks in the spirit of Patcher. All of them only skip client-side rendering work. */
public class Performance extends Module {
    public static Performance INSTANCE;
    private final Setting.Num entityDist = num("Mob Render Distance", 64, 16, 128, 8);
    private final Setting.Num playerDist = num("Player Render Distance", 96, 32, 160, 8);
    private final Setting.Num itemDist = num("Dropped Item Render Distance", 32, 8, 96, 8);
    private final Setting.Num standDist = num("Armor Stand Render Distance", 48, 8, 128, 8);
    private final Setting.Num tileDist = num("Chest / Sign / Skull Render Distance", 48, 16, 96, 8);
    private final Setting.Num particles = num("Particles %", 100, 0, 100, 10);
    private final Setting.Num unfocused = num("FPS While Unfocused", 15, 5, 60, 5);
    public final Setting.Bool noGlErrors = bool("Skip OpenGL Error Checks", true);
    public final Setting.Bool noGlint = bool("Disable Enchantment Glint", false);
    private final Random rng = new Random();

    public Performance() {
        super("Performance", "Render-distance culling and other FPS tweaks.", Category.PERFORMANCE);
        INSTANCE = this;
    }

    /** x/y/z are relative to the camera. */
    public boolean cull(Entity e, double x, double y, double z) {
        if (e == mc.thePlayer || e == mc.getRenderViewEntity()) return false;
        double d = e instanceof EntityPlayer ? playerDist.value
                : e instanceof EntityItem || e instanceof EntityItemFrame ? itemDist.value
                : e instanceof EntityArmorStand ? standDist.value : entityDist.value;
        return x * x + y * y + z * z > d * d;
    }

    public boolean cullTile(double distSq) { return distSq > tileDist.value * tileDist.value; }

    public boolean dropParticle() {
        double p = particles.value;
        return p < 100 && rng.nextInt(100) >= p;
    }

    public int frameLimit(int original) {
        try {
            if (!Display.isActive() && mc.theWorld != null) return Math.min(original, unfocused.i());
        } catch (Throwable ignored) {}
        return original;
    }
}
