package com.kartersanamo.bedwars.api.arena.shop;

import org.bukkit.inventory.ItemStack;

/**
 * Single tier of an upgradable item (e.g. tool or armor level).
 */
public interface IContentTier {

    ItemStack getItem();

    int getCost();
}
