package com.kartersanamo.bedwars.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

/**
 * SQLite implementation of the {@link Database} abstraction.
 */
public final class SQLite extends Database {

    private final File databaseFile;

    public SQLite(final JavaPlugin plugin, final String fileName) {
        super(plugin);
        this.databaseFile = new File(plugin.getDataFolder(), fileName);
    }

    @Override
    public void connect() throws SQLException {
        if (isConnected()) {
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQLite JDBC driver not found", ex);
        }

        if (!databaseFile.getParentFile().exists() && !databaseFile.getParentFile().mkdirs()) {
            logger.warning("Failed to create database directory: " + databaseFile.getParentFile());
        }

        final String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);
    }

    @Override
    public void initSchema() throws SQLException {
        final String sql = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    final_kills INTEGER NOT NULL DEFAULT 0,
                    beds_broken INTEGER NOT NULL DEFAULT 0,
                    games_played INTEGER NOT NULL DEFAULT 0
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public PlayerStats loadPlayerStats(final UUID uniqueId, final String playerName) throws SQLException {
        final String selectSql = "SELECT * FROM player_stats WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final PlayerStats stats = new PlayerStats(uniqueId, resultSet.getString("name"));
                    stats.setWins(resultSet.getInt("wins"));
                    stats.setLosses(resultSet.getInt("losses"));
                    stats.setKills(resultSet.getInt("kills"));
                    stats.setDeaths(resultSet.getInt("deaths"));
                    stats.setFinalKills(resultSet.getInt("final_kills"));
                    stats.setBedsBroken(resultSet.getInt("beds_broken"));
                    stats.setGamesPlayed(resultSet.getInt("games_played"));
                    // Keep name up to date if it changed.
                    stats.setName(playerName);
                    return stats;
                }
            }
        }

        // No existing row; create a new one with default values.
        final PlayerStats stats = new PlayerStats(uniqueId, playerName);
        savePlayerStats(stats);
        return stats;
    }

    @Override
    public void savePlayerStats(final PlayerStats stats) throws SQLException {
        final String sql = """
                INSERT INTO player_stats (uuid, name, wins, losses, kills, deaths, final_kills, beds_broken, games_played)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    wins = excluded.wins,
                    losses = excluded.losses,
                    kills = excluded.kills,
                    deaths = excluded.deaths,
                    final_kills = excluded.final_kills,
                    beds_broken = excluded.beds_broken,
                    games_played = excluded.games_played;
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, stats.getUniqueId().toString());
            statement.setString(2, stats.getName());
            statement.setInt(3, stats.getWins());
            statement.setInt(4, stats.getLosses());
            statement.setInt(5, stats.getKills());
            statement.setInt(6, stats.getDeaths());
            statement.setInt(7, stats.getFinalKills());
            statement.setInt(8, stats.getBedsBroken());
            statement.setInt(9, stats.getGamesPlayed());
            statement.executeUpdate();
        }
    }
}

