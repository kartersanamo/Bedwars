package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.api.arena.EGameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * Holds context for the Play Bed Wars inventory.
 */
public final class PlayBedwarsGuiHolder implements InventoryHolder {

    private final EGameMode gameMode;
    private Inventory inventory;

    public PlayBedwarsGuiHolder(final EGameMode gameMode) {
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