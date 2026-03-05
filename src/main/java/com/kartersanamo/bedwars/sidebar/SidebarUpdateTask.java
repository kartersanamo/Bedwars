package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically refreshes sidebars for all arenas.
 */
public final class SidebarUpdateTask extends BukkitRunnable {

    private final Bedwars plugin;
    private final SidebarService sidebarService;

    public SidebarUpdateTask(final Bedwars plugin, final SidebarService sidebarService) {
        this.plugin = plugin;
        this.sidebarService = sidebarService;
    }

    @Override
    public void run() {
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameState() == com.kartersanamo.bedwars.api.arena.EGameState.IN_GAME
                    || arena.getGameState() == com.kartersanamo.bedwars.api.arena.EGameState.ENDING) {
                if (arena instanceof com.kartersanamo.bedwars.arena.Arena a) {
                    a.checkTierUpgrades();
                    for (org.bukkit.entity.Player player : arena.getPlayers()) {
                        a.ensureSword(player);
                        a.reapplyArmorIfNeeded(player);
                    }
                }
            }
            sidebarService.updateForArena(arena);
        }
    }
}

