package com.kartersanamo.bedwars.upgrades;

import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class UpgradesGuiHolderImpl implements UpgradesGuiHolder {

    private final Arena arena;
    private final ITeam team;
    private Inventory inventory;

    public UpgradesGuiHolderImpl(final Arena arena, final ITeam team) {
        this.arena = arena;
        this.team = team;
    }

    @Override
    public Arena getArena() {
        return arena;
    }

    @Override
    public ITeam getTeam() {
        return team;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
