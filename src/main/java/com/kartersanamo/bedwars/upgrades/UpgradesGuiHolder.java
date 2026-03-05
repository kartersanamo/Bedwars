package com.kartersanamo.bedwars.upgrades;

import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder for the "Upgrades & Traps" GUI. Tracks arena and team so purchases apply to the correct team.
 */
public interface UpgradesGuiHolder extends InventoryHolder {

    Arena getArena();

    ITeam getTeam();
}
