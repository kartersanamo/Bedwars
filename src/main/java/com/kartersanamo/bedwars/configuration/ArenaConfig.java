package com.kartersanamo.bedwars.configuration;

import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents a single arena configuration loaded from {@code arenas/<id>.yml}.
 */
public final class ArenaConfig {

    private final String id;
    private final File file;
    private final YamlConfiguration configuration;
    private final Logger logger;

    private ArenaConfig(final String id, final File file, final YamlConfiguration configuration, final Logger logger) {
        this.id = Objects.requireNonNull(id, "id");
        this.file = Objects.requireNonNull(file, "file");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return configuration.getString(ConfigPath.Arena.WORLD);
    }

    public World getWorld() {
        final String worldName = getWorldName();
        return worldName == null ? null : Bukkit.getWorld(worldName);
    }

    public String getDisplayName() {
        return configuration.getString(ConfigPath.Arena.DISPLAY_NAME, id);
    }

    public boolean isEnabled() {
        return configuration.getBoolean(ConfigPath.Arena.ENABLED, true);
    }

    public int getMinPlayers(final int defaultMinPlayers) {
        return configuration.getInt(ConfigPath.Arena.MIN_PLAYERS, defaultMinPlayers);
    }

    public int getMaxPlayers(final int defaultMaxPlayers) {
        return configuration.getInt(ConfigPath.Arena.MAX_PLAYERS, defaultMaxPlayers);
    }

    public int getTeamSize(final int defaultTeamSize) {
        return configuration.getInt(ConfigPath.Arena.TEAM_SIZE, defaultTeamSize);
    }

    public Location getLobbySpawn() {
        return readLocation(ConfigPath.Arena.LOBBY_SPAWN);
    }

    public Location getSpectatorSpawn() {
        return readLocation(ConfigPath.Arena.SPECTATOR_SPAWN);
    }

    public List<TeamDefinition> getTeamDefinitions() {
        final ConfigurationSection teamsSection = configuration.getConfigurationSection(ConfigPath.Arena.TEAMS);
        if (teamsSection == null) {
            return Collections.emptyList();
        }

        final List<TeamDefinition> result = new ArrayList<>();
        for (String teamId : teamsSection.getKeys(false)) {
            final ConfigurationSection teamSection = teamsSection.getConfigurationSection(teamId);
            if (teamSection == null) {
                continue;
            }

            final String colorName = teamSection.getString(ConfigPath.Arena.TEAM_COLOR);
            if (colorName == null) {
                continue;
            }

            final ETeamColor color;
            try {
                color = ETeamColor.valueOf(colorName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid team color '" + colorName + "' in arena " + id + " for team " + teamId);
                continue;
            }

            final Location spawn = readLocation(ConfigPath.Arena.TEAMS + "." + teamId + "." + ConfigPath.Arena.TEAM_SPAWN);
            final Location bed = readLocation(ConfigPath.Arena.TEAMS + "." + teamId + "." + ConfigPath.Arena.TEAM_BED);

            if (spawn == null || bed == null) {
                logger.warning("Team '" + teamId + "' in arena " + id + " is missing spawn or bed location.");
                continue;
            }

            final List<Location> ironGenerators = readLocationList(ConfigPath.Arena.TEAMS + "." + teamId + "." + ConfigPath.Arena.TEAM_IRON_GENERATORS);
            final List<Location> goldGenerators = readLocationList(ConfigPath.Arena.TEAMS + "." + teamId + "." + ConfigPath.Arena.TEAM_GOLD_GENERATORS);
            final Location shopNpc = readLocation(ConfigPath.Arena.TEAMS + "." + teamId + "." + ConfigPath.Arena.TEAM_SHOP_NPC);

            result.add(new TeamDefinition(teamId, color, spawn, bed, ironGenerators, goldGenerators, shopNpc));
        }

        return result;
    }

    public List<Location> getDiamondGenerators() {
        return readLocationList(ConfigPath.Arena.DIAMOND_GENERATORS);
    }

    public List<Location> getEmeraldGenerators() {
        return readLocationList(ConfigPath.Arena.EMERALD_GENERATORS);
    }

    public Optional<Region> getArenaRegion() {
        final Location pos1 = readLocation(ConfigPath.Arena.ARENA_REGION_POS1);
        final Location pos2 = readLocation(ConfigPath.Arena.ARENA_REGION_POS2);
        if (pos1 == null || pos2 == null) {
            return Optional.empty();
        }
        return Optional.of(new Region(pos1, pos2));
    }

    public void save() throws IOException {
        configuration.save(file);
    }

    public static List<ArenaConfig> loadAll(final File arenasDirectory, final Logger logger) {
        Objects.requireNonNull(arenasDirectory, "arenasDirectory");
        Objects.requireNonNull(logger, "logger");

        if (!arenasDirectory.exists() && !arenasDirectory.mkdirs()) {
            logger.warning("Failed to create arenas directory: " + arenasDirectory.getAbsolutePath());
            return Collections.emptyList();
        }

        final File[] files = arenasDirectory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        final List<ArenaConfig> result = new ArrayList<>();
        for (File file : files) {
            final String name = file.getName();
            final String id = name.substring(0, name.length() - ".yml".length());
            final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            result.add(new ArenaConfig(id, file, yaml, logger));
        }

        return result;
    }

    private Location readLocation(final String path) {
        if (!configuration.isConfigurationSection(path)) {
            return null;
        }
        final String worldName = configuration.getString(path + ".world", getWorldName());
        if (worldName == null) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("World '" + worldName + "' for path '" + path + "' in arena '" + id + "' is not loaded.");
            return null;
        }

        final double x = configuration.getDouble(path + ".x");
        final double y = configuration.getDouble(path + ".y");
        final double z = configuration.getDouble(path + ".z");
        final float yaw = (float) configuration.getDouble(path + ".yaw", 0.0);
        final float pitch = (float) configuration.getDouble(path + ".pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private List<Location> readLocationList(final String path) {
        final List<Location> result = new ArrayList<>();
        final ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            final Location location = readLocation(path + "." + key);
            if (location != null) {
                result.add(location);
            }
        }

        return result;
    }

    public static final class TeamDefinition {
        private final String id;
        private final ETeamColor color;
        private final Location spawn;
        private final Location bed;
        private final List<Location> ironGenerators;
        private final List<Location> goldGenerators;
        private final Location shopNpc;

        public TeamDefinition(final String id,
                              final ETeamColor color,
                              final Location spawn,
                              final Location bed,
                              final List<Location> ironGenerators,
                              final List<Location> goldGenerators,
                              final Location shopNpc) {
            this.id = id;
            this.color = color;
            this.spawn = spawn;
            this.bed = bed;
            this.ironGenerators = Collections.unmodifiableList(new ArrayList<>(ironGenerators));
            this.goldGenerators = Collections.unmodifiableList(new ArrayList<>(goldGenerators));
            this.shopNpc = shopNpc;
        }

        public String getId() {
            return id;
        }

        public ETeamColor getColor() {
            return color;
        }

        public Location getSpawn() {
            return spawn;
        }

        public Location getBed() {
            return bed;
        }

        public List<Location> getIronGenerators() {
            return ironGenerators;
        }

        public List<Location> getGoldGenerators() {
            return goldGenerators;
        }

        public Location getShopNpc() {
            return shopNpc;
        }
    }

    public static final class Region {
        private final Location pos1;
        private final Location pos2;

        public Region(final Location pos1, final Location pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public Location getPos1() {
            return pos1;
        }

        public Location getPos2() {
            return pos2;
        }
    }
}
