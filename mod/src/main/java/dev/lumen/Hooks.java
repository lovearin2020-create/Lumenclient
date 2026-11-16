package dev.lumen;

import dev.lumen.mods.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreObjective;

/**
 * Entry points called from the mixins. Every method is null-safe because some of the patched
 * methods run before the client has finished loading.
 */
public final class Hooks {
    private static boolean on(dev.lumen.core.Module m) { return m != null && m.isEnabled(); }

    public static boolean handleCrash(Throwable t) { return on(CrashPatcher.INSTANCE) && CrashPatcher.INSTANCE.handle(t); }

    public static int frameLimit(int original) { return on(Performance.INSTANCE) ? Performance.INSTANCE.frameLimit(original) : original; }

    public static void firstPersonStart(float partialTicks) { OldAnimations.partialTicks = partialTicks; }

    public static float swingProgress(float original) { return on(OldAnimations.INSTANCE) ? OldAnimations.INSTANCE.swingProgress(original) : original; }

    public static boolean armorTint() { return on(OldAnimations.INSTANCE) && OldAnimations.INSTANCE.armorTint.on(); }

    private static boolean firePushed;

    public static void firePre() {
        if (!on(FireOverlay.INSTANCE)) return;
        firePushed = true;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -FireOverlay.INSTANCE.lower.f(), 0);
    }

    public static void firePost() {
        if (!firePushed) return;
        firePushed = false;
        GlStateManager.popMatrix();
    }

    public static boolean ownNametag(Entity e) {
        Minecraft mc = Minecraft.getMinecraft();
        return on(Nametags.INSTANCE) && e == mc.thePlayer && Minecraft.isGuiEnabled() && !e.isInvisible();
    }

    public static boolean chatNoBackground() { return on(ChatTweaks.INSTANCE) && ChatTweaks.INSTANCE.noBackground.on(); }

    public static int chatHistory(int original) { return on(ChatTweaks.INSTANCE) ? ChatTweaks.INSTANCE.history.i() : original; }

    public static void tooltipPre() { if (on(ScrollableTooltips.INSTANCE)) ScrollableTooltips.INSTANCE.pre(); }

    public static void tooltipPost() { if (ScrollableTooltips.INSTANCE != null) ScrollableTooltips.INSTANCE.post(); }

    public static boolean scoreboard(ScoreObjective o, ScaledResolution r) {
        if (!on(ScoreboardMod.INSTANCE)) return false;
        try { ScoreboardMod.INSTANCE.render(o, r); } catch (Throwable ignored) {}
        return true;
    }

    public static void statsReply() { if (on(PingHud.INSTANCE)) PingHud.INSTANCE.onStatsReply(); }

    /** Hypixel sends scoreboard removals for teams/objectives the client never had; vanilla throws and logs a stack trace for each. */
    public static boolean ignoreNullScoreboardRemoval(Object o) { return o == null; }

    public static boolean skipGlErrorChecks() { return on(Performance.INSTANCE) && Performance.INSTANCE.noGlErrors.on(); }

    public static float hitColor(int component, float original) { return on(HitColor.INSTANCE) ? HitColor.INSTANCE.component(component) : original; }

    /** @return the number of models to draw, or -1 to let vanilla position the item. */
    public static int itemTransform(net.minecraft.entity.item.EntityItem item, double x, double y, double z, float pt, net.minecraft.client.resources.model.IBakedModel model, int count) {
        if (item.getEntityItem() == null || item.getEntityItem().getItem() == null) return -1;
        if (on(ItemPhysics.INSTANCE)) { ItemPhysics.INSTANCE.transform(item, x, y, z, pt, model, count); return count; }
        if (on(Items2D.INSTANCE)) { Items2D.INSTANCE.transform(item, x, y, z, pt, model); return 1; }
        return -1;
    }

    public static String ownNameText(Entity e) {
        return on(Nametags.INSTANCE) && e != null && e == Minecraft.getMinecraft().thePlayer ? Nametags.INSTANCE.replacement() : null;
    }

    public static boolean tabPing(int w, int x, int y, net.minecraft.client.network.NetworkPlayerInfo info) { return on(TabEditor.INSTANCE) && TabEditor.INSTANCE.drawPing(w, x, y, info); }
    public static boolean tabHideHeader() { return on(TabEditor.INSTANCE) && TabEditor.INSTANCE.hideHeader.on(); }
    public static boolean tabHideFooter() { return on(TabEditor.INSTANCE) && TabEditor.INSTANCE.hideFooter.on(); }

    public static boolean hideTitles() { return on(Titles.INSTANCE) && Titles.INSTANCE.hide.on(); }
    public static float titleScale() { return on(Titles.INSTANCE) ? Titles.INSTANCE.scale.f() : 1f; }

    public static boolean hideOwnPotionParticles(Object entity) {
        Minecraft mc = Minecraft.getMinecraft();
        return on(CleanView.INSTANCE) && entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0;
    }

    public static boolean noGlint() { return on(Performance.INSTANCE) && Performance.INSTANCE.noGlint.on(); }

    public static boolean noHurtCam() { return on(HurtCam.INSTANCE); }

    public static boolean cullEntity(Entity e, double x, double y, double z) { return on(Performance.INSTANCE) && e != null && Performance.INSTANCE.cull(e, x, y, z); }

    public static boolean cullTile(double distSq) { return on(Performance.INSTANCE) && Performance.INSTANCE.cullTile(distSq); }

    public static boolean dropParticle() { return on(Performance.INSTANCE) && Performance.INSTANCE.dropParticle(); }
}
