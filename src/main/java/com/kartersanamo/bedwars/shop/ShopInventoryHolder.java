package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Holder for the 54-slot shop GUI. Tracks current view and which tiers are in content slots (9–53).
 */
public interface ShopInventoryHolder extends InventoryHolder {

    /** View id: "quick_buy" or category id (blocks, melee, armor, etc.). */
    String getViewId();

    /** Category when viewing a category; null when viewing Quick Buy. */
    ShopCategory getCategory();

    /** Tiers in order for content slots 9–53. Index 0 = slot 9. */
    List<IContentTier> getContentTiers();

    /** Tier at content slot index (0-based). Content slot = 9 + index. */
    default IContentTier getTierAtContentIndex(int index) {
        List<IContentTier> tiers = getContentTiers();
        if (index < 0 || index >= tiers.size()) return null;
        return tiers.get(index);
    }
}
