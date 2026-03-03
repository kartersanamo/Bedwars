package com.kartersanamo.bedwars.shop.listeners;

import com.kartersanamo.bedwars.shop.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Opens the shop when a player interacts with a shop NPC or villager.
 *
 * For the MVP we keep this simple and open the shop when interacting with
 * any villager; later this can be restricted via configuration.
 */
public final class ShopOpenListener implements Listener {

    private final ShopManager shopManager;

    public ShopOpenListener(final ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Villager)) {
            return;
        }

        final Player player = event.getPlayer();
        shopManager.openShop(player);
        event.setCancelled(true);
    }
}
