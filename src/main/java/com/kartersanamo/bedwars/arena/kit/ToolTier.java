package com.kartersanamo.bedwars.arena.kit;

import org.bukkit.Material;

/** Axe or pickaxe tier. NONE = not purchased yet (shop only). Others degrade one tier on death. */
public enum ToolTier {
    NONE(Material.AIR, Material.AIR),
    WOOD(Material.WOODEN_AXE, Material.WOODEN_PICKAXE),
    STONE(Material.STONE_AXE, Material.STONE_PICKAXE),
    IRON(Material.IRON_AXE, Material.IRON_PICKAXE),
    GOLD(Material.GOLDEN_AXE, Material.GOLDEN_PICKAXE),
    DIAMOND(Material.DIAMOND_AXE, Material.DIAMOND_PICKAXE);

    private final Material axe;
    private final Material pickaxe;

    ToolTier(final Material axe, final Material pickaxe) {
        this.axe = axe;
        this.pickaxe = pickaxe;
    }

    public Material getAxeMaterial() {
        return axe;
    }

    public Material getPickaxeMaterial() {
        return pickaxe;
    }

    public boolean hasTool() {
        return this != NONE;
    }

    /** Axe degrades: diamond->iron, iron->stone, stone->wood, wood->wood, none->none. */
    public ToolTier degradeAxe() {
        return switch (this) {
            case NONE -> NONE;
            case DIAMOND -> IRON;
            case IRON -> STONE;
            case STONE, GOLD, WOOD -> WOOD;
        };
    }

    /** Pickaxe degrades: diamond->gold, gold->iron, iron->wood, wood->wood, none->none. */
    public ToolTier degradePickaxe() {
        return switch (this) {
            case NONE -> NONE;
            case DIAMOND -> GOLD;
            case GOLD -> IRON;
            case IRON -> WOOD;
            case STONE, WOOD -> WOOD;
        };
    }
}
