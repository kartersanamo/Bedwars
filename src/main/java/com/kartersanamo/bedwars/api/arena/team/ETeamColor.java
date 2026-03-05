package com.kartersanamo.bedwars.api.arena.team;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * Standardised team colors used for teams, scoreboards, and items.
 */
public enum ETeamColor {

    RED(ChatColor.RED, DyeColor.RED, Material.RED_WOOL),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, Material.BLUE_WOOL),
    GREEN(ChatColor.GREEN, DyeColor.LIME, Material.LIME_WOOL),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, Material.YELLOW_WOOL),
    AQUA(ChatColor.AQUA, DyeColor.LIGHT_BLUE, Material.LIGHT_BLUE_WOOL),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, Material.WHITE_WOOL),
    PINK(ChatColor.LIGHT_PURPLE, DyeColor.PINK, Material.PINK_WOOL),
    GRAY(ChatColor.GRAY, DyeColor.GRAY, Material.GRAY_WOOL);

    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final Material woolMaterial;

    ETeamColor(final ChatColor chatColor, final DyeColor dyeColor, final Material woolMaterial) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.woolMaterial = woolMaterial;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public DyeColor getDyeColor() {
        return dyeColor;
    }

    public Material getWoolMaterial() {
        return woolMaterial;
    }

    /**
     * Bed material corresponding to this team color.
     */
    public Material getBedMaterial() {
        return switch (this) {
            case RED -> Material.RED_BED;
            case BLUE -> Material.BLUE_BED;
            case GREEN -> Material.LIME_BED;
            case YELLOW -> Material.YELLOW_BED;
            case AQUA -> Material.LIGHT_BLUE_BED;
            case WHITE -> Material.WHITE_BED;
            case PINK -> Material.PINK_BED;
            case GRAY -> Material.GRAY_BED;
        };
    }
}
