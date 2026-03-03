package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Ensures damage logic is only customised for players inside arenas.
 */
public final class DamageListener implements Listener {

    private final Bedwars plugin;

    public DamageListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        // For the MVP we do not need to alter most damage,
        // but this hook is available for future extensions.
    }
}
