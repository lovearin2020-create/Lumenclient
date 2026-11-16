package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Styled version of the vanilla F3+B hitboxes. Boxes are depth-tested and skipped for invisible
 * entities, exactly like vanilla, so nothing is revealed that you could not already see.
 */
public class Hitboxes extends Module {
    private final Setting.Bool players = bool("Players", true);
    private final Setting.Bool mobs = bool("Mobs", false);
    private final Setting.Bool items = bool("Dropped Items", false);
    private final Setting.Bool projectiles = bool("Projectiles", true);
    private final Setting.Col color = col("Color", 0xFFFFFFFF);
    private final Setting.Num width = num("Line Width", 1.5, 1, 4, 0.5);
    private final Setting.Num range = num("Range", 32, 8, 64, 4);

    public Hitboxes() { super("Hitboxes", "Styled entity hitboxes (like F3+B).", Category.VISUAL); }

    private boolean wanted(Entity e) {
        if (e instanceof EntityPlayer) return players.on();
        if (e instanceof EntityLivingBase) return mobs.on();
        if (e instanceof EntityItem) return items.on();
        return (e instanceof EntityArrow || e instanceof EntityFireball || e instanceof EntityThrowable) && projectiles.on();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        int c = color.get();
        double r2 = range.value * range.value;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 1f);
        GL11.glLineWidth(width.f());
        for (Entity en : mc.theWorld.loadedEntityList) {
            if (en == view || en == mc.thePlayer || en.isInvisible() || !wanted(en) || en.getDistanceSqToEntity(view) > r2) continue;
            double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * e.partialTicks;
            double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * e.partialTicks;
            double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * e.partialTicks;
            AxisAlignedBB b = en.getEntityBoundingBox().offset(x - en.posX - vx, y - en.posY - vy, z - en.posZ - vz);
            RenderGlobal.drawSelectionBoundingBox(b);
        }
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
