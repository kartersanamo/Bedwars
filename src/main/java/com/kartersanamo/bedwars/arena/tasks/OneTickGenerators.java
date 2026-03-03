package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task ticking all generators in all active arenas.
 */
public final class OneTickGenerators extends BukkitRunnable {

    private final Bedwars plugin;
    private long tickCounter;

    public OneTickGenerators(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCounter++;
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameState() != EGameState.IN_GAME) {
                continue;
            }
            for (IGenerator generator : arena.getGenerators()) {
                generator.tick(tickCounter);
            }
        }
        plugin.getHologramManager().update(tickCounter);
    }
}
