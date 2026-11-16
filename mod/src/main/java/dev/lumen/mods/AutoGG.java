package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Util;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoGG extends Module {
    /** End-of-game lines used across Hypixel game modes. */
    private static final String[] TRIGGERS = {
            "1st Killer - ", "1st Place - ", "Winner: ", " - Damage Dealt - ", "Winning Team - ", "1st - ",
            "Winners: ", "Winner - ", "Winners - ", "WINNER!", " won the game!", "Top Seeker: ", "1st Place: ",
            "Last team standing!", "Winner #1 (", "Top Survivors", "Most Kills: ", "Your Overall Winstreak:",
            "Reward Summary", "Match Results"
    };
    private final Setting.Text message = text("Message", "gg", 60);
    private final Setting.Num delay = num("Delay (seconds)", 1, 0, 5, 0.5);
    private final Setting.Bool others = bool("Other Servers (plain chat)", false);
    private int ticksLeft = -1;
    private long lastSent;

    public AutoGG() { super("AutoGG", "Says gg when a game ends.", Category.HYPIXEL); }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || ticksLeft >= 0) return;
        boolean hypixel = Util.onHypixel();
        if (!hypixel && !others.on()) return;
        if (System.currentTimeMillis() - lastSent < 10000) return;
        String msg = Util.strip(e.message.getUnformattedText());
        // Ignore player chat ("name: message") so nobody can trigger it by typing a trigger phrase.
        int colon = msg.indexOf(": ");
        boolean playerChat = colon > 0 && colon < 40 && !msg.startsWith(" ") && !msg.startsWith("Winner") && !msg.startsWith("Top Seeker") && !msg.startsWith("1st Place") && !msg.startsWith("Most Kills") && !msg.startsWith("Your Overall");
        if (playerChat) return;
        for (String t : TRIGGERS) {
            if (msg.contains(t)) { ticksLeft = (int) (delay.value * 20); return; }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ticksLeft < 0) return;
        if (ticksLeft-- > 0) return;
        ticksLeft = -1;
        if (mc.thePlayer == null) return;
        lastSent = System.currentTimeMillis();
        String msg = message.value.trim();
        if (msg.isEmpty() || msg.startsWith("/")) return; // never run commands from this box
        mc.thePlayer.sendChatMessage((Util.onHypixel() ? "/ac " : "") + msg);
    }
}
