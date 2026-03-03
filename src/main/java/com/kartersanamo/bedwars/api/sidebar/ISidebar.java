package com.kartersanamo.bedwars.api.sidebar;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Simple abstraction over a scoreboard sidebar for a single player.
 */
public interface ISidebar {

    Player getPlayer();

    void setTitle(String title);

    void setLines(List<String> lines);

    void remove();
}
