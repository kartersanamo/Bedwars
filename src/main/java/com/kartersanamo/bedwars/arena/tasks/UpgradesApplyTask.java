package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.arena.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically applies Haste and Heal Pool effects to players based on team upgrades.
 */
public final class UpgradesApplyTask extends BukkitRunnable {

    private final ArenaManager arenaManager;

    public UpgradesApplyTask(final JavaPlugin plugin, final ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public void run() {
        for (IArena arena : arenaManager.getArenas()) {
            if (!(arena instanceof Arena concreteArena)) continue;
            for (Player player : arena.getPlayers()) {
                concreteArena.applyHasteAndHealPool(player);
            }
        }
    }
}
