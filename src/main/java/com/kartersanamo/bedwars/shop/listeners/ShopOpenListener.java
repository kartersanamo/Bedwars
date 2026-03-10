package com.kartersanamo.bedwars.shop.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.shop.ShopManager;
import com.kartersanamo.bedwars.upgrades.UpgradeManager;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Opens the shop or upgrades GUI when a player right-clicks a villager NPC.
 */
public final class ShopOpenListener implements Listener {

    private static final String ROLE_KEY = "bw_npc_role";
    private static final String ROLE_SHOP = "shop";
    private static final String ROLE_UPGRADES = "upgrades";

    private final ShopManager shopManager;
    private final UpgradeManager upgradeManager;
    private final Bedwars plugin;

    public ShopOpenListener(final Bedwars plugin, final ShopManager shopManager, final UpgradeManager upgradeManager) {
        this.shopManager = shopManager;
        this.upgradeManager = upgradeManager;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        final Player player = event.getPlayer();
        final var arena = plugin != null ? plugin.getArenaManager().getArena(player) : null;
        if (arena == null) {
            event.setCancelled(true);
            return;
        }

        final boolean upgradesNpc = isUpgradesNpc(villager);
        if (upgradesNpc && upgradeManager != null && arena instanceof Arena concreteArena) {
            arena.getTeam(player).ifPresent(team -> upgradeManager.openGui(player, concreteArena, team));
            event.setCancelled(true);
            return;
        }

        shopManager.openShop(player, arena);
        event.setCancelled(true);
    }

    private boolean isUpgradesNpc(final Villager villager) {
        if (plugin != null) {
            final String role = villager.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, ROLE_KEY),
                    PersistentDataType.STRING
            );
            if (ROLE_UPGRADES.equalsIgnoreCase(role)) {
                return true;
            }
            if (ROLE_SHOP.equalsIgnoreCase(role)) {
                return false;
            }
        }

        // Fallback for already-spawned villagers without tags: infer from nearby hologram label.
        for (Entity nearby : villager.getNearbyEntities(2.0, 3.0, 2.0)) {
            if (!(nearby instanceof ArmorStand stand)) {
                continue;
            }
            final String name = stand.getCustomName();
            if (name == null) {
                continue;
            }
            final String stripped = ChatColor.stripColor(name);
            if (stripped.equalsIgnoreCase("UPGRADES")) {
                return true;
            }
            if (stripped.equalsIgnoreCase("ITEM SHOP")) {
                return false;
            }
        }

        return false;
    }
}
