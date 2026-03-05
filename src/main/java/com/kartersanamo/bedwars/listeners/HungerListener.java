package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Prevents players from losing hunger while in a Bedwars arena.
 */
public final class HungerListener implements Listener {

    private final Bedwars plugin;

    public HungerListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.getArenaManager().getArena(player) == null) {
            return;
        }
        // Prevent any hunger loss (new level would be less than current)
        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }
}
