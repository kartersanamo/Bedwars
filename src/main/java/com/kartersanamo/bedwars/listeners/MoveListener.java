package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.arena.Arena;
import org.bukkit.ChatColor;
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

        if (arena.getGameState() != EGameState.IN_GAME && arena.getGameState() != EGameState.ENDING) {
            return;
        }
        if (arena instanceof Arena concreteArena) {
            concreteArena.checkTrapTrigger(player);
        }
        final int voidY = plugin.getMainConfig().getVoidY();
        if (player.getLocation().getY() < voidY) {
            final String voidMessage = ChatColor.RED + player.getName() + ChatColor.GRAY + " fell into the void.";
            for (Player p : arena.getPlayers()) {
                p.sendMessage(voidMessage);
            }
            for (Player p : arena.getSpectators()) {
                p.sendMessage(voidMessage);
            }
            DeathListener.handleArenaDeath(plugin, player, null, arena);
        }
    }
}
