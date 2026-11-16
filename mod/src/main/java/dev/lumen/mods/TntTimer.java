package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TntTimer extends Module {
    private final Setting.Bool bedwars = bool("Hypixel Bed Wars Fuse Fix", true);

    public TntTimer() { super("TNT Timer", "Countdown above primed TNT.", Category.VISUAL); }

    /** Hypixel Bed Wars TNT explodes after 2.6s, but the client is told the vanilla 4s fuse. */
    private int offset() {
        if (!bedwars.on() || !Util.onHypixel() || mc.theWorld == null) return 0;
        try {
            ScoreObjective o = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
            if (o != null && Util.strip(o.getDisplayName()).toUpperCase().contains("BED WARS")) return 28;
        } catch (Throwable ignored) {}
        return 0;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (mc.theWorld == null) return;
        int off = offset();
        for (Entity en : mc.theWorld.loadedEntityList) {
            if (!(en instanceof EntityTNTPrimed)) continue;
            EntityTNTPrimed tnt = (EntityTNTPrimed) en;
            float sec = Math.max(0, (tnt.fuse - off - e.partialTicks) / 20f);
            int color = sec > 2 ? 0xFF55FF55 : sec > 1 ? 0xFFFFFF55 : 0xFFFF5555;
            double x = tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) * e.partialTicks;
            double y = tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) * e.partialTicks;
            double z = tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) * e.partialTicks;
            R.worldLabel(String.format("%.1f", sec), x, y + 1.35, z, color);
        }
    }
}
