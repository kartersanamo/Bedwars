package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Lobby countdown task before the game begins.
 */
public final class GameStartingTask extends BukkitRunnable {

    private final IArena arena;
    private int secondsRemaining;

    public GameStartingTask(final IArena arena, final int seconds) {
        this.arena = arena;
        this.secondsRemaining = seconds;
    }

    @Override
    public void run() {
        if (arena.getGameState() != EGameState.STARTING) {
            cancel();
            return;
        }

        final int currentPlayers = arena.getPlayers().size();
        final int minPlayers = arena.getMinPlayers();
        final int maxPlayers = arena.getMaxPlayers();

        // Cancel countdown if we ever drop below min players or 75% capacity.
        final int threshold = (int) Math.ceil(maxPlayers * 0.75D);
        if (currentPlayers < minPlayers || currentPlayers < threshold) {
            arena.setGameState(EGameState.LOBBY_WAITING);
            for (Player player : arena.getPlayers()) {
                player.sendMessage(ChatColor.YELLOW + "Countdown cancelled due to insufficient players.");
            }
            cancel();
            return;
        }

        // If the lobby reaches 100% capacity and we still have more than 10s left,
        // accelerate the countdown to 10 seconds.
        if (currentPlayers >= maxPlayers && secondsRemaining > 10) {
            secondsRemaining = 10;
        }

        if (secondsRemaining <= 0) {
            arena.forceStart();
            cancel();
            return;
        }

        // Announce only milestone seconds in chat.
        if (secondsRemaining == 15
                || secondsRemaining == 10
                || secondsRemaining == 5
                || secondsRemaining == 3
                || secondsRemaining == 2
                || secondsRemaining == 1) {
            final String message = ChatColor.GREEN + "Game starting in " + secondsRemaining + " second"
                    + (secondsRemaining == 1 ? "" : "s") + "...";
            for (Player player : arena.getPlayers()) {
                player.sendMessage(message);
            }
        }

        secondsRemaining--;
    }
}
