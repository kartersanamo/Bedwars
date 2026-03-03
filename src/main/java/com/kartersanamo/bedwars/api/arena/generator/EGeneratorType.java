package com.kartersanamo.bedwars.api.arena.generator;

import org.bukkit.Material;

/**
 * Types of resource generators available in an arena.
 */
public enum EGeneratorType {

    IRON(Material.IRON_INGOT),
    GOLD(Material.GOLD_INGOT),
    DIAMOND(Material.DIAMOND),
    EMERALD(Material.EMERALD);

    private final Material dropMaterial;

    EGeneratorType(final Material dropMaterial) {
        this.dropMaterial = dropMaterial;
    }

    public Material getDropMaterial() {
        return dropMaterial;
    }
}
