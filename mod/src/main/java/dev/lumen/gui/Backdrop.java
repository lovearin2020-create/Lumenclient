package dev.lumen.gui;

import dev.lumen.util.R;

/** Animated aurora background shared by the title screen and the mod menu when no world is loaded. */
public final class Backdrop {
    private static final long START = System.currentTimeMillis();
    private static final int[] ORBS = {0x554C8DFF, 0x4D8B5CFF, 0x4038D9F5, 0x38FF6BCB};

    public static void draw(int w, int h, int mx, int my) {
        float t = (System.currentTimeMillis() - START) / 1000f;
        R.gradient(0, 0, w, h, 0xFF070A16, 0xFF0F1630);
        float px = (mx - w / 2f) / w, py = (my - h / 2f) / h;
        float big = Math.max(w, h);
        for (int i = 0; i < ORBS.length; i++) {
            float x = w * (0.5f + 0.38f * (float) Math.sin(t * 0.11f + i * 1.9f)) - px * 30;
            float y = h * (0.5f + 0.32f * (float) Math.cos(t * 0.09f + i * 2.4f)) - py * 30;
            R.glow(x, y, big * (0.38f + 0.06f * (float) Math.sin(t * 0.2f + i)), ORBS[i]);
        }
        // drifting sparks with a little mouse parallax
        for (int i = 0; i < 70; i++) {
            float sx = hash(i * 3 + 1), sy = hash(i * 3 + 2), depth = 0.3f + hash(i * 3 + 3) * 0.7f;
            float x = (sx * w + (float) Math.sin(t * 0.3f * depth + i) * 12 - px * 40 * depth + w) % w;
            float y = ((sy * h - t * 9f * depth) % h + h) % h - py * 40 * depth;
            float tw = 0.45f + 0.55f * (float) Math.sin(t * (0.8f + depth) + i * 1.3f);
            int a = (int) (40 + 150 * depth * tw);
            R.circle(x, y, 0.6f + depth * 1.1f, (Math.max(0, Math.min(255, a)) << 24) | 0xDDE8FF);
        }
        R.gradient(0, h * 0.55f, w, h * 0.45f, 0x00000000, 0x66000000);
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }
}
