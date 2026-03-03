package com.kartersanamo.bedwars.api.arena;

public enum EGameMode {
    SOLO(1, "Solos"),
    DOUBLES(2, "Doubles"),
    THREES(3, "3v3v3v3"),
    FOURS(4, "4v4v4v4");

    private final int teamSize;
    private final String displayName;

    EGameMode(final int teamSize, final String displayName) {
        this.teamSize = teamSize;
        this.displayName = displayName;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public String getDisplayName() {
        return displayName;
    }
}

