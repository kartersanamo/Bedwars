package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.database.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles the short ending period and then resets the arena.
 */
public final class GameRestartingTask extends BukkitRunnable {

    private final Bedwars plugin;
    private final IArena arena;
    private int ticksRemaining;

    public GameRestartingTask(final Bedwars plugin, final IArena arena, final int seconds) {
        this.plugin = plugin;
        this.arena = arena;
        this.ticksRemaining = seconds * 20;
    }

    @Override
    public void run() {
        if (arena.getGameState() != EGameState.ENDING && arena.getGameState() != EGameState.RESETTING) {
            cancel();
            return;
        }

        if (ticksRemaining > 0) {
            ticksRemaining -= 20;
            return;
        }

        arena.setGameState(EGameState.RESETTING);

        final Database database = plugin.getDatabase();
        for (Player player : arena.getPlayers()) {
            try {
                final PlayerStats stats = database.getCachedStats(player.getUniqueId(), player.getName());
                stats.incrementGamesPlayed();
                // For simplicity we treat surviving players as winners.
                stats.incrementWins();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to update end-of-game stats: " + ex.getMessage());
            }

            player.getInventory().clear();
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            plugin.getSidebarService().removeSidebar(player);
            if (plugin.getMainConfig().getLobbySpawn() != null) {
                player.teleport(plugin.getMainConfig().getLobbySpawn());
            }
            // So the player can join another game without /bw leave.
            plugin.getArenaManager().playerLeftArena(player);
        }
        for (Player spectator : arena.getSpectators()) {
            plugin.getSidebarService().removeSidebar(spectator);
            if (plugin.getMainConfig().getLobbySpawn() != null) {
                spectator.teleport(plugin.getMainConfig().getLobbySpawn());
            }
            plugin.getArenaManager().playerLeftArena(spectator);
        }

        // Persist stats after each game.
        database.flushCache();

        plugin.getInternalAdapter().restoreArena(arena);
        arena.resetAfterGame();

        cancel();
    }
}
