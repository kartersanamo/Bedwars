package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.database.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player deaths inside arenas and delegates to arena logic.
 */
public final class DeathListener implements Listener {

    private final Bedwars plugin;

    public DeathListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        final Player killer = player.getKiller();
        arena.handlePlayerDeath(player, killer);

        final Database database = plugin.getDatabase();
        try {
            final PlayerStats victimStats = database.getCachedStats(player.getUniqueId(), player.getName());
            victimStats.incrementDeaths();

            if (killer != null && killer != player) {
                final PlayerStats killerStats = database.getCachedStats(killer.getUniqueId(), killer.getName());
                killerStats.incrementKills();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to update kill/death stats: " + ex.getMessage());
        }
    }
}
