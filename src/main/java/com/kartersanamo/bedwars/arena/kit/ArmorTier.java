package com.kartersanamo.bedwars.arena.kit;

import org.bukkit.Material;

/**
 * Leggings/boots armor upgrade tier. Chest and helmet are always leather (team color).
 */
public enum ArmorTier {
    LEATHER(Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
    CHAINMAIL(Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
    IRON(Material.IRON_LEGGINGS, Material.IRON_BOOTS),
    DIAMOND(Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS);

    private final Material leggings;
    private final Material boots;

    ArmorTier(final Material leggings, final Material boots) {
        this.leggings = leggings;
        this.boots = boots;
    }

    public Material getLeggings() {
        return leggings;
    }

    public Material getBoots() {
        return boots;
    }
}
