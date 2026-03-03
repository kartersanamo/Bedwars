package com.kartersanamo.bedwars.shop.listeners;

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

    private final ShopManager shopManager;

    public ShopInventoryListener(final ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Inventory inventory = event.getView().getTopInventory();
        final String title = event.getView().getTitle();

        if (!title.startsWith(ROOT_TITLE)) {
            return;
        }

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Never allow taking items from the shop inventories.
        event.setCancelled(true);

        if (ROOT_TITLE.equals(title)) {
            handleRootClick(player, clicked);
        } else if (title.startsWith(CATEGORY_TITLE_PREFIX)) {
            final String categoryId = ChatColor.stripColor(title.substring(CATEGORY_TITLE_PREFIX.length())).toLowerCase();
            handleCategoryClick(player, categoryId, clicked);
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

    private void handleCategoryClick(final Player player, final String categoryId, final ItemStack clicked) {
        // MVP: single category "blocks" with a single wool tier purchasable using iron ingots.
        if (!"blocks".equalsIgnoreCase(categoryId)) {
            return;
        }

        if (clicked.getType() != Material.WHITE_WOOL) {
            return;
        }

        // Cost is 4 iron ingots for 16 wool (matches ShopManager default).
        final int cost = 4;
        final Material currency = Material.IRON_INGOT;

        int totalCurrency = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency) {
                totalCurrency += stack.getAmount();
            }
        }

        if (totalCurrency < cost) {
            player.sendMessage(ChatColor.RED + "You do not have enough iron to purchase this.");
            return;
        }

        // Remove the cost.
        int remaining = cost;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            final ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() != currency) {
                continue;
            }
            if (stack.getAmount() > remaining) {
                stack.setAmount(stack.getAmount() - remaining);
                player.getInventory().setItem(i, stack);
                remaining = 0;
            } else {
                remaining -= stack.getAmount();
                player.getInventory().clear(i);
            }
        }

        // Give the player the purchased wool.
        player.getInventory().addItem(new ItemStack(Material.WHITE_WOOL, 16));
    }
}

