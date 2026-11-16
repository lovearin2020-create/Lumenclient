package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BlockOverlay extends Module {
    private final Setting.Col outline = col("Outline Color", 0xFF4C8DFF);
    private final Setting.Num width = num("Outline Width", 2, 1, 6, 0.5);
    private final Setting.Bool fill = bool("Fill", true);
    private final Setting.Num fillOpacity = num("Fill Opacity", 20, 5, 80, 5);

    public BlockOverlay() { super("Block Overlay", "Custom block outline and fill.", Category.VISUAL); }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent e) {
        MovingObjectPosition m = e.target;
        if (m == null || m.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mc.theWorld == null) return;
        BlockPos pos = m.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air) return;
        e.setCanceled(true);
        EntityPlayer p = e.player;
        double dx = p.lastTickPosX + (p.posX - p.lastTickPosX) * e.partialTicks;
        double dy = p.lastTickPosY + (p.posY - p.lastTickPosY) * e.partialTicks;
        double dz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * e.partialTicks;
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos).expand(0.002, 0.002, 0.002).offset(-dx, -dy, -dz);
        int c = outline.get();
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (fill.on()) {
            GlStateManager.disableCull();
            GlStateManager.color(r, g, b, fillOpacity.f() / 100f);
            box(bb);
            GlStateManager.enableCull();
        }
        GlStateManager.color(r, g, b, 1f);
        GL11.glLineWidth(width.f());
        RenderGlobal.drawSelectionBoundingBox(bb);
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void box(AxisAlignedBB b) {
        GL11.glBegin(GL11.GL_QUADS);
        v(b.minX, b.minY, b.minZ); v(b.maxX, b.minY, b.minZ); v(b.maxX, b.minY, b.maxZ); v(b.minX, b.minY, b.maxZ);
        v(b.minX, b.maxY, b.minZ); v(b.minX, b.maxY, b.maxZ); v(b.maxX, b.maxY, b.maxZ); v(b.maxX, b.maxY, b.minZ);
        v(b.minX, b.minY, b.minZ); v(b.minX, b.maxY, b.minZ); v(b.maxX, b.maxY, b.minZ); v(b.maxX, b.minY, b.minZ);
        v(b.minX, b.minY, b.maxZ); v(b.maxX, b.minY, b.maxZ); v(b.maxX, b.maxY, b.maxZ); v(b.minX, b.maxY, b.maxZ);
        v(b.minX, b.minY, b.minZ); v(b.minX, b.minY, b.maxZ); v(b.minX, b.maxY, b.maxZ); v(b.minX, b.maxY, b.minZ);
        v(b.maxX, b.minY, b.minZ); v(b.maxX, b.maxY, b.minZ); v(b.maxX, b.maxY, b.maxZ); v(b.maxX, b.minY, b.maxZ);
        GL11.glEnd();
    }

    private static void v(double x, double y, double z) { GL11.glVertex3d(x, y, z); }
}
