package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles void deaths by monitoring player Y position.
 */
public final class MoveListener implements Listener {

    private final Bedwars plugin;

    public MoveListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        final int voidY = plugin.getMainConfig().getVoidY();
        if (player.getLocation().getY() < voidY) {
            // Trigger a standard death; the DeathListener will take over.
            player.setHealth(0.0D);
        }
    }
}
