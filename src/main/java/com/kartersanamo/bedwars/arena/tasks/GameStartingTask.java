package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
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

        if (arena.getPlayers().size() < arena.getMinPlayers()) {
            arena.setGameState(EGameState.LOBBY_WAITING);
            cancel();
            return;
        }

        if (secondsRemaining <= 0) {
            arena.forceStart();
            cancel();
            return;
        }

        for (Player player : arena.getPlayers()) {
            player.sendTitle("Game starting", secondsRemaining + "s", 0, 20, 0);
        }

        secondsRemaining--;
    }
}
