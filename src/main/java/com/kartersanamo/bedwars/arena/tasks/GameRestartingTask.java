package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.database.PlayerStats;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        final List<Player> players = new ArrayList<>(arena.getPlayers());
        final List<Player> spectators = new ArrayList<>(arena.getSpectators());

        for (Player player : players) {
            try {
                final PlayerStats stats = database.getCachedStats(player.getUniqueId(), player.getName());
                stats.incrementGamesPlayed();
                // For simplicity we treat surviving players as winners.
                stats.incrementWins();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to update end-of-game stats: " + ex.getMessage());
            }

            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getDefaultValue());
            player.setFoodLevel(20);
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getSidebarService().removeSidebar(player);
            if (plugin.getMainConfig().getLobbySpawn() != null) {
                player.teleport(plugin.getMainConfig().getLobbySpawn());
            }
            arena.removePlayer(player, false);
            plugin.getArenaManager().playerLeftArena(player);
        }
        for (Player spectator : spectators) {
            plugin.getSidebarService().removeSidebar(spectator);
            spectator.getInventory().setHelmet(null);
            spectator.getInventory().setChestplate(null);
            spectator.getInventory().setLeggings(null);
            spectator.getInventory().setBoots(null);
            spectator.setHealth(Objects.requireNonNull(spectator.getAttribute(Attribute.MAX_HEALTH)).getDefaultValue());
            spectator.setFoodLevel(20);
            spectator.setGameMode(GameMode.SURVIVAL);
            if (plugin.getMainConfig().getLobbySpawn() != null) {
                spectator.teleport(plugin.getMainConfig().getLobbySpawn());
            }
            arena.removePlayer(spectator, false);
            plugin.getArenaManager().playerLeftArena(spectator);
        }

        // Persist stats after each game.
        database.flushCache();

        plugin.getInternalAdapter().restoreArena(arena);
        arena.resetAfterGame();

        cancel();
    }
}
