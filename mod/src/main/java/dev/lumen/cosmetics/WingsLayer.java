package dev.lumen.cosmetics;

import dev.lumen.mods.Wings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Procedurally drawn, flapping wings on your own player. Other players never see them. */
public class WingsLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** Wing outline in pixels: bone joint followed by the finger tips, clockwise. */
    private static final float[][] TIPS = {{9, -10}, {22, -14}, {25, -3}, {19, 6}, {10, 10}, {3, 8}};

    private static final ResourceLocation DRAGON = new ResourceLocation("textures/entity/enderdragon/dragon.png");

    private final RenderPlayer renderer;
    private final DragonWing dragon = new DragonWing();

    public WingsLayer(RenderPlayer renderer) { this.renderer = renderer; }

    /** The Ender Dragon's own wing (bone + membrane, with a jointed tip), using the vanilla dragon texture. */
    private static final class DragonWing extends ModelBase {
        final ModelRenderer wing, tip;
        DragonWing() {
            textureWidth = 256; textureHeight = 256;
            setTextureOffset("wing.bone", 112, 88);
            setTextureOffset("wing.skin", -56, 88);
            setTextureOffset("wingtip.bone", 112, 136);
            setTextureOffset("wingtip.skin", -56, 144);
            wing = new ModelRenderer(this, "wing");
            wing.setTextureSize(256, 256);
            wing.setRotationPoint(-2.0F, 0.0F, 0.0F);
            wing.addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8);
            wing.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
            tip = new ModelRenderer(this, "wingtip");
            tip.setTextureSize(256, 256);
            tip.setRotationPoint(-56.0F, 0.0F, 0.0F);
            tip.addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4);
            tip.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
            wing.addChild(tip);
        }
    }

    private void renderDragon(AbstractClientPlayer p, Wings w, float age) {
        float f = age * 0.1f * w.speed.f() * (p.onGround ? 1f : 2.2f);
        float k = 0.2f * w.size.f();
        GlStateManager.pushMatrix();
        if (p.isSneaking()) {
            GlStateManager.translate(0, 0.2f, 0);
            GlStateManager.rotate(28.6f, 1, 0, 0);
        }
        GlStateManager.translate(0, 0.2f, 0.14f);
        GlStateManager.scale(k, k, k);
        if (w.tint.on()) {
            int c = w.color.get();
            GlStateManager.color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 1f);
        } else GlStateManager.color(1, 1, 1, 1);
        renderer.bindTexture(DRAGON);
        GlStateManager.enableCull();
        for (int side = 0; side < 2; side++) {
            dragon.wing.rotateAngleX = (float) Math.toRadians(-80) - (float) Math.cos(f) * 0.2f;
            dragon.wing.rotateAngleY = (float) Math.toRadians(20) + (float) Math.sin(f) * 0.4f;
            dragon.wing.rotateAngleZ = (float) Math.toRadians(20);
            dragon.tip.rotateAngleZ = -(float) (Math.sin(f + 2.0f) + 0.5) * 0.75f;
            dragon.wing.render(0.0625f);
            GlStateManager.scale(-1, 1, 1);
            if (side == 0) GlStateManager.cullFace(GL11.GL_FRONT);
        }
        GlStateManager.cullFace(GL11.GL_BACK);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt, float age, float yaw, float pitch, float scale) {
        Wings w = Wings.INSTANCE;
        if (w == null || !w.isEnabled() || p != mc.thePlayer || p.isInvisible()) return;
        if (w.style.is("Ender Dragon")) { renderDragon(p, w, age); return; }
        int c = w.color.get();
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        boolean airborne = !p.onGround;
        float flap = (float) Math.sin(age * 0.12f * w.speed.f() * (airborne ? 2.2f : 1f));
        float angle = 28f + flap * (airborne ? 26f : 12f);
        float s = 0.0625f * w.size.f();

        GlStateManager.pushMatrix();
        if (p.isSneaking()) {
            GlStateManager.translate(0, 0.2f, 0);
            GlStateManager.rotate(28.6f, 1, 0, 0);
        }
        GlStateManager.translate(0, 0.16f, 0.135f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        for (int side = -1; side <= 1; side += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(side * 0.06f, 0, 0);
            GlStateManager.scale(side * s, s, s);
            GlStateManager.rotate(-angle, 0, 1, 0);
            // membrane
            GlStateManager.color(r, g, b, w.opacity.f() / 100f);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex3f(0, 0, 0);
            for (float[] t : TIPS) GL11.glVertex3f(t[0], t[1], 0);
            GL11.glEnd();
            // bones
            GlStateManager.color(r * 0.55f, g * 0.55f, b * 0.55f, 1f);
            GL11.glLineWidth(2.5f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3f(0, 0, 0); GL11.glVertex3f(TIPS[0][0], TIPS[0][1], 0);
            for (int i = 1; i < TIPS.length - 1; i++) {
                GL11.glVertex3f(TIPS[0][0], TIPS[0][1], 0);
                GL11.glVertex3f(TIPS[i][0], TIPS[i][1], 0);
            }
            GL11.glEnd();
            // outline
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (float[] t : TIPS) GL11.glVertex3f(t[0], t[1], 0);
            GL11.glVertex3f(0, 0, 0);
            GL11.glEnd();
            GL11.glLineWidth(1f);
            GlStateManager.popMatrix();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    public boolean shouldCombineTextures() { return false; }
}
