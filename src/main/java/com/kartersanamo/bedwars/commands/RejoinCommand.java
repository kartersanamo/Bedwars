package com.kartersanamo.bedwars.commands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.RejoinManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Handles the /rejoin command.
 */
public final class RejoinCommand implements CommandExecutor {

    private final Bedwars plugin;

    public RejoinCommand(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command command, final @NonNull String label, final String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        final RejoinManager.Entry entry = plugin.getRejoinManager().getEntry(player);
        if (entry == null) {
            player.sendMessage(ChatColor.RED + "There is no Bed Wars game to rejoin.");
            return true;
        }

        final IArena arena = entry.arena;
        final EGameState state = arena.getGameState();
        if (state == EGameState.ENDING || state == EGameState.RESETTING || state == EGameState.LOBBY_WAITING) {
            plugin.getRejoinManager().clearEntry(player);
            player.sendMessage(ChatColor.RED + "That Bed Wars game has already ended.");
            return true;
        }

        final long offlineMillis = System.currentTimeMillis() - entry.disconnectedAt;
        final boolean timedOut = offlineMillis > RejoinManager.OFFLINE_LIMIT_MILLIS;

        final Optional<ITeam> teamOpt = arena.getTeam(player);
        final ITeam team = teamOpt.orElse(null);
        final boolean bedDestroyed = team == null || team.isBedDestroyed();

        if (timedOut || bedDestroyed) {
            // Rejoin as spectator.
            plugin.getRejoinManager().clearEntry(player);
            player.sendMessage(ChatColor.RED + "You rejoined as a spectator.");
            player.teleport(arena.getSpectatorSpawn());
            player.setGameMode(GameMode.SPECTATOR);
            return true;
        }

        // Rejoin as active player with a short freeze/protection.
        plugin.getRejoinManager().clearEntry(player);

        player.sendMessage(ChatColor.YELLOW + "Rejoining your Bed Wars game...");
        player.teleport(team.getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 4, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 10, false, false, true));

        // Simple 5-second title countdown.
        for (int i = 5; i >= 1; i--) {
            final int seconds = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.sendTitle(
                    ChatColor.GREEN + "Rejoining...",
                    ChatColor.YELLOW + "You can move in " + ChatColor.RED + seconds + ChatColor.YELLOW + " second" + (seconds == 1 ? "" : "s") + ".",
                    0, 20, 0
            ), (5 - seconds) * 20L);
        }

        // After countdown, remove slow effect but keep them in the game.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.sendTitle(ChatColor.GREEN + "Good luck!", "", 0, 20, 0);
        }, 5 * 20L);

        return true;
    }
}

