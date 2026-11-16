package dev.lumen.util;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;

/** Rolling one-second click counters for the CPS and Keystrokes mods. */
public final class Clicks {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<Long>(), RIGHT = new ArrayDeque<Long>();

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        if (e.button == 0) LEFT.add(System.currentTimeMillis());
        else if (e.button == 1) RIGHT.add(System.currentTimeMillis());
    }

    private static int count(ArrayDeque<Long> q) {
        long limit = System.currentTimeMillis() - 1000L;
        while (!q.isEmpty() && q.peekFirst() < limit) q.pollFirst();
        return q.size();
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }
}
