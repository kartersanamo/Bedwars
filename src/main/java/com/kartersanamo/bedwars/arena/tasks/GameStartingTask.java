package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
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

        // Cancel countdown if we ever drop below min players or the threshold
        final int threshold = (int) Math.ceil(maxPlayers * arena.getLobbyThreshold());
        if (currentPlayers < minPlayers || currentPlayers < threshold) {
            arena.setGameState(EGameState.LOBBY_WAITING);
            for (Player player : arena.getPlayers()) {
                player.sendMessage(ChatColor.RED + "Countdown cancelled due to insufficient players.");
            }
            cancel();
            return;
        }

        // If the lobby reaches 100% capacity, and we still have more than 10s left,
        // speed up the countdown to 10 seconds.
        if (currentPlayers >= maxPlayers && secondsRemaining > 10) {
            secondsRemaining = 10;
        }

        if (secondsRemaining <= 0) {
            arena.forceStart();
            cancel();
            return;
        }

        // Announce milestone seconds in chat and show big number as title.
        if (secondsRemaining == 20
                || secondsRemaining == 15
                || secondsRemaining == 10
                || secondsRemaining == 5
                || secondsRemaining == 4
                || secondsRemaining == 3
                || secondsRemaining == 2
                || secondsRemaining == 1) {
            final ChatColor numberColor = secondsRemaining <= 5 ? ChatColor.RED : secondsRemaining == 10 ? ChatColor.GOLD: ChatColor.AQUA;
            final String unit = secondsRemaining == 1 ? " second!" : " seconds!";
            final String message = ChatColor.YELLOW + "The game starts in " + numberColor + secondsRemaining
                    + ChatColor.YELLOW + unit;
            for (Player player : arena.getPlayers()) {
                player.sendMessage(message);
                player.sendTitle(
                        numberColor.toString() + secondsRemaining,
                        null,
                        5, 25, 5
                );
            }
        }

        secondsRemaining--;
    }
}
