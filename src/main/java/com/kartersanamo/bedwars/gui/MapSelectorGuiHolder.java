package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.api.arena.EGameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * Holds context for the Map Selector inventory.
 */
public final class MapSelectorGuiHolder implements InventoryHolder {

    private final EGameMode gameMode;
    private Inventory inventory;

    public MapSelectorGuiHolder(final EGameMode gameMode) {
        this.gameMode = gameMode;
    }

    public EGameMode getGameMode() {
        return gameMode;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public Inventory getInventory() {
        return Objects.requireNonNull(inventory, "inventory");
    }
}