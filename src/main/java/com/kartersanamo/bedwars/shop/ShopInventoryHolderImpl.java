package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.inventory.Inventory;

import java.util.List;

final class ShopInventoryHolderImpl implements ShopInventoryHolder {

    private final String viewId;
    private final ShopCategory category;
    private final List<IContentTier> contentTiers;
    private Inventory inventory;

    ShopInventoryHolderImpl(final String viewId, final ShopCategory category, final List<IContentTier> contentTiers) {
        this.viewId = viewId;
        this.category = category;
        // Allow null entries so specific content slots can intentionally be empty
        // (e.g., Quick Buy row placeholders). Copy into a mutable list to avoid
        // callers depending on immutability.
        this.contentTiers = contentTiers != null ? new java.util.ArrayList<>(contentTiers) : new java.util.ArrayList<>();
    }

    void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public ShopCategory getCategory() {
        return category;
    }

    @Override
    public List<IContentTier> getContentTiers() {
        return contentTiers;
    }
}
