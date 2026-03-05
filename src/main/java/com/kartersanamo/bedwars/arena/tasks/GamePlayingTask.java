package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.arena.Arena;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically checks win conditions while the game is running.
 */
public final class GamePlayingTask extends BukkitRunnable {

    private final IArena arena;

    public GamePlayingTask(final IArena arena) {
        this.arena = arena;
    }

    @Override
    public void run() {
        if (arena.getGameState() != EGameState.IN_GAME) {
            cancel();
            return;
        }
        if (arena instanceof Arena concreteArena) {
            concreteArena.checkGameOver();
        }
    }
}
