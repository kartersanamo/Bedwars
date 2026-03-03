package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.shop.main.BuyItem;
import com.kartersanamo.bedwars.shop.main.CategoryContent;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal shop manager providing a single in-game shop GUI.
 */
public final class ShopManager {

    private final Map<String, ShopCategory> categories = new HashMap<>();

    public ShopManager() {
        loadDefaultShop();
    }

    private void loadDefaultShop() {
        // Simple category: Blocks
        final ShopCategory blocks = new ShopCategory("blocks", namedItem(Material.WHITE_WOOL, "Blocks"));

        final List<com.kartersanamo.bedwars.api.arena.shop.IContentTier> woolTiers = new ArrayList<>();
        woolTiers.add(new SimpleContentTier(new ItemStack(Material.WHITE_WOOL, 16), 4));
        final CategoryContent woolContent = new CategoryContent("wool", woolTiers);

        blocks.addContent(woolContent);

        categories.put(blocks.getId(), blocks);
    }

    public void openShop(final Player player) {
        final Inventory inventory = Bukkit.createInventory(player, 27, "Item Shop");

        int slot = 0;
        for (ShopCategory category : categories.values()) {
            inventory.setItem(slot++, category.getIcon());
        }

        player.openInventory(inventory);
    }

    private ItemStack namedItem(final Material material, final String name) {
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static final class SimpleContentTier implements com.kartersanamo.bedwars.api.arena.shop.IContentTier {
        private final ItemStack item;
        private final int cost;

        private SimpleContentTier(final ItemStack item, final int cost) {
            this.item = item;
            this.cost = cost;
        }

        @Override
        public ItemStack getItem() {
            return item.clone();
        }

        @Override
        public int getCost() {
            return cost;
        }
    }
}
