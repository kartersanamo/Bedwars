package com.kartersanamo.bedwars.database;

import java.util.UUID;

/**
 * Simple in-memory representation of persistent player statistics.
 */
public final class PlayerStats {

    private final UUID uniqueId;
    private String name;

    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int finalKills;
    private int bedsBroken;
    private int gamesPlayed;

    public PlayerStats(final UUID uniqueId, final String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void incrementLosses() {
        losses++;
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void incrementDeaths() {
        deaths++;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public void incrementFinalKills() {
        finalKills++;
    }

    public int getBedsBroken() {
        return bedsBroken;
    }

    public void incrementBedsBroken() {
        bedsBroken++;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void incrementGamesPlayed() {
        gamesPlayed++;
    }

    public void setWins(final int wins) {
        this.wins = wins;
    }

    public void setLosses(final int losses) {
        this.losses = losses;
    }

    public void setKills(final int kills) {
        this.kills = kills;
    }

    public void setDeaths(final int deaths) {
        this.deaths = deaths;
    }

    public void setFinalKills(final int finalKills) {
        this.finalKills = finalKills;
    }

    public void setBedsBroken(final int bedsBroken) {
        this.bedsBroken = bedsBroken;
    }

    public void setGamesPlayed(final int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
}

