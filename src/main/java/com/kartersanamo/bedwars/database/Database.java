package com.kartersanamo.bedwars.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Abstract database facade used by the plugin.
 *
 * The MVP implementation uses a local SQLite database, but the abstraction
 * allows future implementations (for example MySQL) without affecting callers.
 */
public abstract class Database {

    protected final JavaPlugin plugin;
    protected final Logger logger;

    protected Connection connection;

    // Simple in-memory cache to avoid repeated DB hits during a game.
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();

    protected Database(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
    }

    /**
     * Opens a database connection if not already open.
     */
    public abstract void connect() throws SQLException;

    /**
     * Returns the underlying JDBC connection, or {@code null} if not connected.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Initialises the database schema (creating tables if missing).
     */
    public abstract void initSchema() throws SQLException;

    /**
     * Gracefully closes the database connection.
     */
    public void disconnect() {
        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException ex) {
            logger.warning("Failed to close database connection: " + ex.getMessage());
        } finally {
            connection = null;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    /**
     * Loads the statistics for the given player, creating an empty record if none exists yet.
     */
    public abstract PlayerStats loadPlayerStats(UUID uniqueId, String playerName) throws SQLException;

    /**
     * Persists the given statistics.
     */
    public abstract void savePlayerStats(PlayerStats stats) throws SQLException;

    public PlayerStats getCachedStats(final UUID uniqueId, final String playerName) throws SQLException {
        PlayerStats cached = statsCache.get(uniqueId);
        if (cached != null) {
            cached.setName(playerName);
            return cached;
        }
        cached = loadPlayerStats(uniqueId, playerName);
        statsCache.put(uniqueId, cached);
        return cached;
    }

    public void flushCache() {
        for (PlayerStats stats : statsCache.values()) {
            try {
                savePlayerStats(stats);
            } catch (SQLException ex) {
                logger.warning("Failed to save stats for " + stats.getUniqueId() + ": " + ex.getMessage());
            }
        }
        statsCache.clear();
    }
}
