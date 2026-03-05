package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Holder for the 54-slot shop GUI. Tracks current view and which tiers are in
 * the logical content slots (rows 3–5, columns 2–8, excluding borders).
 */
public interface ShopInventoryHolder extends InventoryHolder {

    /** View id: "quick_buy" or category id (blocks, melee, armor, etc.). */
    String getViewId();

    /** Category when viewing a category; null when viewing Quick Buy. */
    ShopCategory getCategory();

    /**
     * Tiers in order for logical content slots. Index 0 corresponds to the
     * first item slot (row 3, column 2), then left-to-right, top-to-bottom.
     */
    List<IContentTier> getContentTiers();

    /** Tier at content slot index (0-based). Content slot = 9 + index. */
    default IContentTier getTierAtContentIndex(int index) {
        List<IContentTier> tiers = getContentTiers();
        if (index < 0 || index >= tiers.size()) return null;
        return tiers.get(index);
    }
}
