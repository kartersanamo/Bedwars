package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Records disconnects from arenas so players can use /rejoin.
 */
public final class RejoinListener implements Listener {

    private final Bedwars plugin;

    public RejoinListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }
        final EGameState state = arena.getGameState();
        if (state != EGameState.IN_GAME && state != EGameState.STARTING && state != EGameState.LOBBY_WAITING) {
            return;
        }
        // Only mark as potential rejoin if they were actually participating.
        plugin.getRejoinManager().recordDisconnect(player, arena);
    }
}

