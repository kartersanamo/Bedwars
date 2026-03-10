package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.Bedwars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * One-time fallback repair after the first player joins, when lobby chunks/entities
 * are most likely to be loaded.
 */
public final class NPCStartupRepairListener implements Listener {

    private final Bedwars plugin;
    private boolean attempted;

    public NPCStartupRepairListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (attempted || plugin.getNpcManager() == null) {
            return;
        }
        attempted = true;

        final Player player = event.getPlayer();
        // Delay slightly so chunk/entity state around spawn has settled.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            final int before = plugin.getNpcManager().getAllNPCs().size();
            if (before > 0) {
                return;
            }
            final int after = plugin.getNpcManager().repairRuntimeMappings();
            plugin.getLogger().info("NPC startup join-repair by " + player.getName() + " — before: " + before + ", after: " + after);
        }, 40L);
    }
}

