package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.stream.Collectors;

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

        final List<ITeam> aliveTeams = arena.getTeams().stream()
                .filter(team -> !team.isEliminated())
                .collect(Collectors.toList());

        if (aliveTeams.size() <= 1) {
            arena.setGameState(EGameState.ENDING);

            if (!aliveTeams.isEmpty()) {
                final ITeam winningTeam = aliveTeams.get(0);
                for (Player player : winningTeam.getOnlineMembers()) {
                    player.sendTitle("Victory!", "", 10, 60, 10);
                }
            }

            // Schedule arena reset after a short delay.
            new GameRestartingTask(Bedwars.getInstance(), arena, 10)
                    .runTaskTimer(Bedwars.getInstance(), 20L, 20L);

            cancel();
        }
    }
}
