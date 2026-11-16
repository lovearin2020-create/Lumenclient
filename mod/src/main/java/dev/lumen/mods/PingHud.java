package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Hypixel and many other networks report every player's tab-list ping as 1 ms, so by default the
 * ping is measured: every few seconds the client asks the server for its statistics (the same
 * request the Statistics screen sends) and times the reply.
 */
public class PingHud extends TextHud {
    public static PingHud INSTANCE;
    private final Setting.Mode source = mode("Source", "Measured", "Measured", "Tab List");
    private final Setting.Num interval = num("Measure Every (seconds)", 3, 2, 15, 1);
    private final long[] samples = new long[4];
    private int count, ticks;
    private volatile boolean pending;
    private volatile long sentAt;

    public PingHud() {
        super("Ping", "Your latency to the server.", START, 4, START, 80);
        INSTANCE = this;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !source.is("Measured")) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) { count = 0; pending = false; return; }
        ticks++;
        if (pending && System.nanoTime() - sentAt > 5000000000L) pending = false; // reply lost
        if (pending || ticks < interval.i() * 20) return;
        ticks = 0;
        sentAt = System.nanoTime();
        pending = true;
        mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
    }

    /** Called from the network thread the moment the statistics reply arrives. */
    public void onStatsReply() {
        if (!pending) return;
        pending = false;
        samples[count++ % samples.length] = (System.nanoTime() - sentAt) / 1000000L;
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { count = 0; ticks = 0; pending = false; }

    protected String text(boolean editing) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return editing ? "0 ms" : null;
        if (source.is("Measured")) {
            if (mc.isSingleplayer()) return "0 ms";
            if (count == 0) return "... ms";
            // The reply waits for the next server tick, so the lowest recent sample is the closest to the real ping.
            long best = Long.MAX_VALUE;
            for (int i = 0; i < Math.min(count, samples.length); i++) best = Math.min(best, samples[i]);
            return best + " ms";
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info == null ? 0 : info.getResponseTime()) + " ms";
    }
}
