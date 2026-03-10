package com.kartersanamo.bedwars.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * Holds paging context for the admin arena browser GUI.
 */
public final class AdminArenaBrowserHolder implements InventoryHolder {

    private final int page;
    private Inventory inventory;

    public AdminArenaBrowserHolder(final int page) {
        this.page = Math.max(0, page);
    }

    public int getPage() {
        return page;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return Objects.requireNonNull(inventory, "inventory");
    }
}