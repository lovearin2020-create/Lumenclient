package dev.lumen.core;

import dev.lumen.mods.MenuBlur;
import dev.lumen.mods.MotionBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

/** Owns the game's single post-processing slot for Menu Blur and Motion Blur. */
public final class Shaders {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation BLUR = new ResourceLocation("lumen", "shaders/post/menu_blur.json");
    private static final ResourceLocation MOTION = new ResourceLocation("lumen", "shaders/post/motion_blur.json");
    private ResourceLocation current;
    private long retryAt;
    private float lastAmount = -1;
    private Field listField;

    private static boolean fastRender() {
        try { return (Boolean) Class.forName("Config").getMethod("isFastRender").invoke(null); } catch (Throwable t) { return false; }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        ResourceLocation want = null;
        if (mc.theWorld != null && OpenGlHelper.shadersSupported && !fastRender()) {
            boolean menu = mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat);
            if (menu && MenuBlur.INSTANCE != null && MenuBlur.INSTANCE.isEnabled()) want = BLUR;
            else if (MotionBlur.INSTANCE != null && MotionBlur.INSTANCE.isEnabled()) want = MOTION;
        }
        try {
            if (want == null) {
                if (current != null) { mc.entityRenderer.stopUseShader(); current = null; }
                return;
            }
            if (want != current || !mc.entityRenderer.isShaderActive()) {
                if (System.currentTimeMillis() < retryAt) return;
                mc.entityRenderer.loadShader(want);
                if (mc.entityRenderer.isShaderActive()) { current = want; lastAmount = -1; }
                else { current = null; retryAt = System.currentTimeMillis() + 5000; }
            }
            if (current == MOTION) {
                float amount = 0.35f + MotionBlur.INSTANCE.amount.f() * 0.07f;
                if (amount != lastAmount) { setAmount(amount); lastAmount = amount; }
            }
        } catch (Throwable t) {
            current = null;
            retryAt = System.currentTimeMillis() + 5000;
        }
    }

    @SuppressWarnings("unchecked")
    private void setAmount(float amount) throws Exception {
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        if (group == null) return;
        if (listField == null) listField = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
        for (Shader s : (List<Shader>) listField.get(group)) {
            ShaderUniform u = s.getShaderManager().getShaderUniform("Amount");
            if (u != null) u.set(amount);
        }
    }
}
