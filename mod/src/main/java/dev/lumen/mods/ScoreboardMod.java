package dev.lumen.mods;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collection;
import java.util.List;

public class ScoreboardMod extends Module {
    public static ScoreboardMod INSTANCE;
    private final Setting.Bool hide = bool("Hide Scoreboard", false);
    private final Setting.Bool numbers = bool("Hide Red Numbers", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool shadow = bool("Text Shadow", false);
    private final Setting.Num scale = num("Scale", 1.0, 0.5, 1.5, 0.05);

    public ScoreboardMod() {
        super("Scoreboard", "Restyle, resize or hide the sidebar.", Category.HUD);
        INSTANCE = this;
    }

    /** Replacement for the vanilla sidebar renderer. */
    public void render(ScoreObjective objective, ScaledResolution res) {
        if (hide.on()) return;
        FontRenderer fr = mc.fontRendererObj;
        Scoreboard sb = objective.getScoreboard();
        Collection<Score> sorted = sb.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(sorted, new Predicate<Score>() {
            public boolean apply(Score s) { return s.getPlayerName() != null && !s.getPlayerName().startsWith("#"); }
        }));
        if (list.size() > 15) list = Lists.newArrayList(Iterables.skip(list, list.size() - 15));

        boolean nums = !numbers.on();
        int w = fr.getStringWidth(objective.getDisplayName());
        for (Score s : list) {
            ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
            if (nums) line += ": " + EnumChatFormatting.RED + s.getScorePoints();
            w = Math.max(w, fr.getStringWidth(line));
        }
        int fh = fr.FONT_HEIGHT;
        int total = list.size() * fh;
        float k = scale.f();
        // Anchor to the right edge, vertically centred like vanilla.
        GlStateManager.pushMatrix();
        GlStateManager.translate(res.getScaledWidth(), res.getScaledHeight() / 2f, 0);
        GlStateManager.scale(k, k, 1);
        int bottom = total / 3;
        int right = -1, left = right - w - 4;
        int j = 0;
        for (Score s : list) {
            j++;
            ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
            String name = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
            int y = bottom - j * fh;
            if (background.on()) Gui.drawRect(left, y, right, y + fh, 0x50000000);
            fr.drawString(name, left + 2, y, 0xFFFFFFFF, shadow.on());
            if (nums) {
                String pts = EnumChatFormatting.RED + "" + s.getScorePoints();
                fr.drawString(pts, right - fr.getStringWidth(pts), y, 0xFFFFFFFF, shadow.on());
            }
            if (j == list.size()) {
                String title = objective.getDisplayName();
                if (background.on()) {
                    Gui.drawRect(left, y - fh - 1, right, y - 1, 0x60000000);
                    Gui.drawRect(left, y - 1, right, y, 0x50000000);
                }
                fr.drawString(title, left + 2 + (w - fr.getStringWidth(title)) / 2f, y - fh, 0xFFFFFFFF, shadow.on());
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }
}
