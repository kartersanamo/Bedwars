package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Formats chat messages for players in Bedwars arenas to show their team color.
 *
 * Format:
 * [COLOR] name: message
 *
 * Where:
 * - [COLOR] is in the team's ChatColor
 * - "name: message" is in light gray
 */
public final class ChatListener implements Listener {

    private final Bedwars plugin;

    public ChatListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return; // Non-Bedwars chat uses default formatting.
        }

        final ITeam team = arena.getTeam(player).orElse(null);
        final ChatColor teamColor = team != null ? team.getColor().getChatColor() : ChatColor.GRAY;
        final String colorName = team != null ? team.getColor().name() : "NONE";

        final String prefix = teamColor + "[" + colorName + "] ";
        final String format = prefix + ChatColor.GRAY + "%1$s: %2$s";

        event.setFormat(format);
    }
}

