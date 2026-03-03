package com.kartersanamo.bedwars.configuration;

import com.kartersanamo.bedwars.api.configuration.ConfigManager;
import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Strongly-typed wrapper around {@code config.yml}.
 */
public final class MainConfig {

    private final FileConfiguration configuration;
    private final Logger logger;

    public MainConfig(final ConfigManager configManager, final Logger logger) {
        this.configuration = Objects.requireNonNull(configManager, "configManager")
                .getConfig("config.yml");
        this.logger = Objects.requireNonNull(logger, "logger");
        applyDefaults();
    }

    private void applyDefaults() {
        configuration.addDefault(ConfigPath.Main.LOBBY_WORLD, "world");

        configuration.addDefault(ConfigPath.Main.ARENA_MIN_PLAYERS, 2);
        configuration.addDefault(ConfigPath.Main.ARENA_MAX_PLAYERS, 16);
        configuration.addDefault(ConfigPath.Main.ARENA_TEAM_SIZE, 4);

        configuration.addDefault(ConfigPath.Main.GAME_RESPAWN_DELAY_SECONDS, 5);
        configuration.addDefault(ConfigPath.Main.GAME_VOID_Y, 0);

        configuration.addDefault(ConfigPath.Main.DATABASE_TYPE, "sqlite");
        configuration.addDefault(ConfigPath.Main.DATABASE_SQLITE_FILE, "bedwars.db");

        configuration.addDefault(ConfigPath.Main.DEBUG, false);

        configuration.options().copyDefaults(true);
    }

    public String getLobbyWorldName() {
        return configuration.getString(ConfigPath.Main.LOBBY_WORLD, "world");
    }

    public World getLobbyWorld() {
        final String worldName = getLobbyWorldName();
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Configured lobby world '" + worldName + "' is not loaded.");
        }
        return world;
    }

    public Location getLobbySpawn() {
        final World world = getLobbyWorld();
        if (world == null) {
            return null;
        }

        if (!configuration.isConfigurationSection(ConfigPath.Main.LOBBY_SPAWN)) {
            return world.getSpawnLocation();
        }

        final double x = configuration.getDouble(ConfigPath.Main.LOBBY_SPAWN + ".x", world.getSpawnLocation().getX());
        final double y = configuration.getDouble(ConfigPath.Main.LOBBY_SPAWN + ".y", world.getSpawnLocation().getY());
        final double z = configuration.getDouble(ConfigPath.Main.LOBBY_SPAWN + ".z", world.getSpawnLocation().getZ());
        final float yaw = (float) configuration.getDouble(ConfigPath.Main.LOBBY_SPAWN + ".yaw", world.getSpawnLocation().getYaw());
        final float pitch = (float) configuration.getDouble(ConfigPath.Main.LOBBY_SPAWN + ".pitch", world.getSpawnLocation().getPitch());

        return new Location(world, x, y, z, yaw, pitch);
    }

    public int getDefaultMinPlayers() {
        return configuration.getInt(ConfigPath.Main.ARENA_MIN_PLAYERS, 2);
    }

    public int getDefaultMaxPlayers() {
        return configuration.getInt(ConfigPath.Main.ARENA_MAX_PLAYERS, 16);
    }

    public int getDefaultTeamSize() {
        return configuration.getInt(ConfigPath.Main.ARENA_TEAM_SIZE, 4);
    }

    public int getRespawnDelaySeconds() {
        return configuration.getInt(ConfigPath.Main.GAME_RESPAWN_DELAY_SECONDS, 5);
    }

    public int getVoidY() {
        return configuration.getInt(ConfigPath.Main.GAME_VOID_Y, 0);
    }

    public String getDatabaseType() {
        return configuration.getString(ConfigPath.Main.DATABASE_TYPE, "sqlite");
    }

    public String getSqliteFileName() {
        return configuration.getString(ConfigPath.Main.DATABASE_SQLITE_FILE, "bedwars.db");
    }

    public boolean isDebugEnabled() {
        return configuration.getBoolean(ConfigPath.Main.DEBUG, false);
    }
}
