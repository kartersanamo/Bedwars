package com.kartersanamo.bedwars.shop.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.shop.ShopInventoryHolder;
import com.kartersanamo.bedwars.shop.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Handles interactions in the 54-slot shop GUI: category row (0–8) switches view,
 * content slots (9–53) purchase or add to Quick Buy.
 */
public final class ShopInventoryListener implements Listener {

    private static final int NAV_ROW_END = 8;

    // Must match ShopManager.CONTENT_SLOTS.
    private static final int[] CONTENT_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

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

        if (!(event.getInventory().getHolder() instanceof ShopInventoryHolder holder)) {
            return;
        }

        event.setCancelled(true);

        final int rawSlot = event.getRawSlot();
        final Inventory top = event.getView().getTopInventory();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }

        // Category row (top row).
        if (rawSlot <= NAV_ROW_END) {
            handleCategoryRowClick(player, rawSlot);
            return;
        }

        // Content area: only specific slots are valid; divider panes and borders are ignored.
        int contentIndex = -1;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == rawSlot) {
                contentIndex = i;
                break;
            }
        }
        if (contentIndex == -1) {
            return;
        }

        final IContentTier tier = holder.getTierAtContentIndex(contentIndex);
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (event.isShiftClick()) {
            // Shift-click behavior:
            // - In any category view: add clicked tier to player's Quick Buy row.
            // - In Quick Buy view, last row (indices 14–20): toggle/remove that slot.
            final String viewId = holder.getViewId();
            if ("quick_buy".equalsIgnoreCase(viewId) && contentIndex >= 14) {
                final int quickIndex = contentIndex - 14;
                shopManager.clearQuickBuySlot(player, quickIndex);
                shopManager.openView(player, "quick_buy", arena);
            } else if (tier != null) {
                shopManager.addToQuickBuy(player, tier);
                // If currently looking at Quick Buy, refresh so the row updates immediately.
                if ("quick_buy".equalsIgnoreCase(viewId)) {
                    shopManager.openView(player, "quick_buy", arena);
                }
            }
        } else if (tier != null) {
            tryPurchase(player, tier, arena);
        }
    }

    private void handleCategoryRowClick(final Player player, final int navSlot) {
        final String[] navOrder = shopManager.getNavOrder();
        if (navSlot < 0 || navSlot >= navOrder.length) return;
        final String viewId = navOrder[navSlot];
        final IArena arena = plugin.getArenaManager().getArena(player);
        shopManager.openView(player, viewId, arena);
    }

    private void tryPurchase(final Player player, final IContentTier tier, final IArena arena) {
        final int cost = tier.getCostFor(arena);
        final Material currency = tier.getCurrency();

        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency) {
                total += stack.getAmount();
            }
        }

        if (total < cost) {
            final int needMore = cost - total;
            player.sendMessage(ChatColor.RED + "You don't have enough " + formatCurrencyDisplay(currency) + "! Need " + needMore + " more!");
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
        if (arena instanceof Arena concreteArena) {
            concreteArena.applyTeamUpgradesToInventory(player);
        }
        final String itemName = formatItemName(tier.getItem());
        player.sendMessage(ChatColor.GREEN + "You purchased " + ChatColor.GOLD + itemName);
    }

    private static String formatItemName(final ItemStack item) {
        if (item != null && item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        if (item != null && item.getType() != Material.AIR) {
            final String[] words = item.getType().name().toLowerCase().split("_");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) sb.append(" ");
                if (!words[i].isEmpty()) {
                    sb.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
                }
            }
            return sb.toString();
        }
        return "Item";
    }

    private static String formatCurrencyDisplay(final Material m) {
        if (m == Material.IRON_INGOT) return "Iron";
        if (m == Material.GOLD_INGOT) return "Gold";
        if (m == Material.DIAMOND) return "Diamond";
        if (m == Material.EMERALD) return "Emerald";
        final String n = m.name().toLowerCase().replace("_", " ");
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

}
