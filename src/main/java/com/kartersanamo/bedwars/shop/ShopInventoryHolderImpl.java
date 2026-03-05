package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.List;

final class ShopInventoryHolderImpl implements ShopInventoryHolder {

    private final String viewId;
    private final ShopCategory category;
    private final List<IContentTier> contentTiers;
    private Inventory inventory;

    ShopInventoryHolderImpl(final String viewId, final ShopCategory category, final List<IContentTier> contentTiers) {
        this.viewId = viewId;
        this.category = category;
        this.contentTiers = contentTiers != null ? List.copyOf(contentTiers) : Collections.emptyList();
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
