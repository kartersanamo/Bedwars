package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.api.sidebar.ISidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic scoreboard implementation for a single player.
 */
public final class BWSidebar implements ISidebar {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    private List<String> lines = new ArrayList<>();

    public BWSidebar(final Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("bedwars", "dummy", "Bedwars");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void setTitle(final String title) {
        objective.setDisplayName(title);
    }

    @Override
    public void setLines(final List<String> lines) {
        this.lines = new ArrayList<>(lines);
        redraw();
    }

    @Override
    public void remove() {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void redraw() {
        // Clear existing entries.
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int score = lines.size();
        for (String line : lines) {
            final Score s = objective.getScore(line);
            s.setScore(score--);
        }
    }
}
