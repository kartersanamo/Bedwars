package com.kartersanamo.bedwars.shop.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.shop.ShopManager;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles interactions within the Bedwars shop inventories.
 */
public final class ShopInventoryListener implements Listener {

    private static final String ROOT_TITLE = "Item Shop";
    private static final String CATEGORY_TITLE_PREFIX = ROOT_TITLE + " - ";

    private final Bedwars plugin;
    private final ShopManager shopManager;

    public ShopInventoryListener(final Bedwars plugin, final ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final String title = event.getView().getTitle();
        if (!title.startsWith(ROOT_TITLE)) {
            return;
        }

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        event.setCancelled(true);

        if (ROOT_TITLE.equals(title)) {
            handleRootClick(player, clicked);
        } else if (title.startsWith(CATEGORY_TITLE_PREFIX)) {
            final String categoryId = ChatColor.stripColor(title.substring(CATEGORY_TITLE_PREFIX.length())).trim().toLowerCase();
            final int slot = event.getRawSlot();
            final Inventory top = event.getView().getTopInventory();
            if (slot >= 0 && slot < top.getSize()) {
                final ShopCategory category = shopManager.getCategory(categoryId);
                handleCategoryClick(player, category, slot);
            }
        }
    }

    private void handleRootClick(final Player player, final ItemStack clicked) {
        for (ShopCategory category : shopManager.getCategories()) {
            if (category.getIcon().isSimilar(clicked)) {
                shopManager.openCategory(player, category);
                return;
            }
        }
    }

    private void handleCategoryClick(final Player player, final ShopCategory category, final int slot) {
        if (category == null) return;

        final IContentTier tier = shopManager.getTierAtSlot(category, slot);
        if (tier == null) return;

        final IArena arena = plugin.getArenaManager().getArena(player);
        final int cost = tier.getCostFor(arena);
        final Material currency = tier.getCurrency();

        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency) {
                total += stack.getAmount();
            }
        }

        if (total < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough " + formatCurrency(currency) + " (need " + cost + ").");
            return;
        }
        if (!tier.canPurchase(player, arena)) {
            player.sendMessage(ChatColor.RED + "You already have this upgrade or better.");
            return;
        }

        int remaining = cost;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            final ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() != currency) continue;
            if (stack.getAmount() > remaining) {
                stack.setAmount(stack.getAmount() - remaining);
                player.getInventory().setItem(i, stack);
                remaining = 0;
            } else {
                remaining -= stack.getAmount();
                player.getInventory().clear(i);
            }
        }

        tier.giveReward(player, arena);
        player.sendMessage(ChatColor.GREEN + "Purchased!");
    }

    private static String formatCurrency(final Material m) {
        if (m == Material.IRON_INGOT) return "iron";
        if (m == Material.GOLD_INGOT) return "gold";
        if (m == Material.DIAMOND) return "diamonds";
        if (m == Material.EMERALD) return "emeralds";
        return m.name().toLowerCase();
    }
}
