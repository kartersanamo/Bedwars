package com.kartersanamo.bedwars.api.arena.shop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.kartersanamo.bedwars.api.arena.IArena;

/**
 * Single tier of a shop item (blocks, tools, armor, etc.).
 */
public interface IContentTier {

    ItemStack getItem();

    int getCost();

    /**
     * Cost when purchasing in the given arena (e.g. diamond sword 3 in 3's/4's, 4 in solos/doubles).
     * Default is getCost().
     */
    default int getCostFor(IArena arena) {
        return getCost();
    }

    Material getCurrency();

    /**
     * Whether this purchase is allowed (e.g. armor cannot downgrade). Default true.
     */
    default boolean canPurchase(Player player, IArena arena) {
        return true;
    }

    /**
     * Gives the reward after purchase (item or upgrade). Call only after currency was removed.
     */
    void giveReward(Player player, IArena arena);
}
