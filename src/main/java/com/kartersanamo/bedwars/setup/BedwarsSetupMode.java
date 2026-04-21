package com.kartersanamo.bedwars.setup;

import org.bukkit.Material;

import java.util.EnumSet;

/**
 * Preset game modes offered in the setup wizard. Values match {@code arenas/*.yml} mode keys.
 */
public enum BedwarsSetupMode {
    SOLO("solo", "Solo", 1, 2, 8, Material.WHITE_WOOL,
            "Each team is one player. Typical 2–8 players."),
    DOUBLES("doubles", "Doubles", 2, 4, 16, Material.LIGHT_BLUE_WOOL,
            "Two players per team. Typical 4–16 players."),
    THREES("threes", "Threes", 3, 6, 24, Material.YELLOW_WOOL,
            "Three per team. Typical 6–24 players."),
    FOURS("fours", "Fours", 4, 8, 32, Material.ORANGE_WOOL,
            "Four per team — standard Bedwars. Typical 8–32 players.");

    private final String yamlKey;
    private final String label;
    private final int teamSize;
    private final int minPlayers;
    private final int maxPlayers;
    private final Material iconMaterial;
    private final String blurb;

    BedwarsSetupMode(final String yamlKey, final String label, final int teamSize,
                     final int minPlayers, final int maxPlayers, final Material iconMaterial,
                     final String blurb) {
        this.yamlKey = yamlKey;
        this.label = label;
        this.teamSize = teamSize;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.iconMaterial = iconMaterial;
        this.blurb = blurb;
    }

    public String getYamlKey() {
        return yamlKey;
    }

    public String getLabel() {
        return label;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public String getBlurb() {
        return blurb;
    }

    public static EnumSet<BedwarsSetupMode> allEnabledByDefault() {
        return EnumSet.allOf(BedwarsSetupMode.class);
    }
}
