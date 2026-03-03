package com.kartersanamo.bedwars.api.arena.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a purchasable item in the shop.
 */
public interface IBuyItem {

    ItemStack getDisplayItem();

    int getCost();

    /**
     * Currency material type is determined implicitly from the ItemStack type
     * (iron, gold, diamond, emerald).
     */
    ItemStack getCurrencyItem();

    boolean canPurchase(Player player);

    void purchase(Player player);
}
