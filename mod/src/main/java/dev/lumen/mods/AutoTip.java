package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Util;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoTip extends Module {
    private final Setting.Num interval = num("Interval (minutes)", 15, 5, 60, 5);
    private final Setting.Bool hide = bool("Hide Tip Messages", true);
    private long nextTip;
    private boolean wasOnHypixel;

    public AutoTip() { super("AutoTip", "Runs /tip all on a timer.", Category.HYPIXEL); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        boolean on = mc.thePlayer != null && Util.onHypixel();
        long now = System.currentTimeMillis();
        if (on && !wasOnHypixel) nextTip = now + 15000;
        wasOnHypixel = on;
        if (on && now >= nextTip) {
            nextTip = now + (long) (interval.value * 60000);
            mc.thePlayer.sendChatMessage("/tip all");
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!hide.on() || e.type == 2 || !Util.onHypixel()) return;
        String m = Util.strip(e.message.getUnformattedText());
        if (m.startsWith("You tipped ") || m.startsWith("You already tipped everyone") || m.startsWith("No one has a network booster active")
                || m.startsWith("You've already tipped someone in the past hour")) {
            e.setCanceled(true);
        }
    }
}
