package com.kartersanamo.bedwars.shop.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.shop.ShopManager;
import com.kartersanamo.bedwars.upgrades.UpgradeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Opens the shop or upgrades GUI when a player right-clicks a villager NPC.
 */
public final class ShopOpenListener implements Listener {

    private final ShopManager shopManager;
    private final UpgradeManager upgradeManager;
    private final Bedwars plugin;

    public ShopOpenListener(final ShopManager shopManager) {
        this.shopManager = shopManager;
        this.upgradeManager = null;
        this.plugin = null;
    }

    public ShopOpenListener(final Bedwars plugin, final ShopManager shopManager, final UpgradeManager upgradeManager) {
        this.shopManager = shopManager;
        this.upgradeManager = upgradeManager;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Villager)) {
            return;
        }

        final Player player = event.getPlayer();
        final var arena = plugin != null ? plugin.getArenaManager().getArena(player) : null;
        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        final String name = event.getRightClicked().getCustomName();
        if (name != null && name.contains("Upgrades") && upgradeManager != null && arena instanceof Arena concreteArena) {
            arena.getTeam(player).ifPresent(team -> upgradeManager.openGui(player, concreteArena, team));
            event.setCancelled(true);
            return;
        }

        shopManager.openShop(player, arena);
        event.setCancelled(true);
    }
}
