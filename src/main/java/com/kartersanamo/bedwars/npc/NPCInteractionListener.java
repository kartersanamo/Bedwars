package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.gui.PlayBedwarsGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listener for NPC interactions.
 */
public final class NPCInteractionListener implements Listener {

    private final Bedwars plugin;

    public NPCInteractionListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final NPCSpawner spawner = plugin.getNpcManager().getNPCByEntity(event.getRightClicked());
        if (spawner == null) {
            return;
        }

        event.setCancelled(true);

        final Player player = event.getPlayer();
        if (plugin.getNpcManager().isInRemoveMode(player)) {
            plugin.getNpcManager().removeNPCByEntity(event.getRightClicked());
            player.sendMessage(ChatColor.GREEN + "NPC removed!");
            return;
        }

        PlayBedwarsGui.openFor(player, spawner.getGameMode());
    }
}
