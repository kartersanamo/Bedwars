package com.kartersanamo.bedwars.setup;

import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Helpers to read/write arena YAML in the same shape {@link com.kartersanamo.bedwars.configuration.ArenaConfig} expects.
 */
public final class ArenaSetupYamlIO {

    private ArenaSetupYamlIO() {}

    public static YamlConfiguration loadOrCreate(final File file) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Could not create directory: " + file.getParentFile());
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Could not create file: " + file);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Writes a location in the same shape as hand-authored arena files (e.g. {@code Airshow.yml}):
     * whole block coordinates as integers, fractional coords as doubles, world + yaw/pitch.
     * Yaw and pitch are snapped to the nearest 90° for spawns and NPCs.
     */
    public static void writeLocation(final YamlConfiguration yaml, final String path, final Location loc) {
        Objects.requireNonNull(loc.getWorld(), "world");
        final float yaw = snapYawCardinal(loc.getYaw());
        final float pitch = snapPitchCardinal(loc.getPitch());
        yaml.set(path + ".world", loc.getWorld().getName());
        setYamlNumber(yaml, path + ".x", loc.getX());
        setYamlNumber(yaml, path + ".y", loc.getY());
        setYamlNumber(yaml, path + ".z", loc.getZ());
        yaml.set(path + ".yaw", (double) yaw);
        yaml.set(path + ".pitch", (double) pitch);
    }

    /** Whole numbers as {@code int} (matches {@code x: -107}); otherwise {@code double}. */
    private static void setYamlNumber(final YamlConfiguration yaml, final String key, final double v) {
        if (Double.isFinite(v) && v == Math.rint(v) && v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
            yaml.set(key, (int) Math.rint(v));
        } else {
            yaml.set(key, v);
        }
    }

    /** Horizontal facing: nearest of south (0°), west (90°), north (±180°), east (-90°). */
    public static float snapYawCardinal(final float yaw) {
        float y = yaw;
        y = (y + 180f) % 360f;
        if (y < 0f) {
            y += 360f;
        }
        y -= 180f;
        return Math.round(y / 90f) * 90f;
    }

    /** Vertical facing: nearest of straight down (90°), level (0°), straight up (-90°). Clamped to [-90, 90]. */
    public static float snapPitchCardinal(final float pitch) {
        final float clamped = Math.max(-90f, Math.min(90f, pitch));
        float snapped = Math.round(clamped / 90f) * 90f;
        snapped = Math.max(-90f, Math.min(90f, snapped));
        return snapped;
    }

    /** Short label for chat after saving a facing (uses Spigot yaw: 0 south, 90 west, ±180 north, -90 east). */
    public static String describeCardinalFacing(final float yaw, final float pitch) {
        final String horiz = switch ((int) snapYawCardinal(yaw)) {
            case 0 -> "south";
            case 90, -270 -> "west";
            case 180, -180 -> "north";
            case -90, 270 -> "east";
            default -> snapYawCardinal(yaw) + "°";
        };
        final String vert = switch ((int) snapPitchCardinal(pitch)) {
            case -90 -> "looking up";
            case 90 -> "looking down";
            default -> "level";
        };
        return horiz + ", " + vert;
    }

    /** Adds {@code d1}, {@code d2}, … under {@code diamond-generators} (same as reference arena YAML). */
    public static void appendDiamondGenerator(final YamlConfiguration yaml, final Location loc) {
        final String listPath = ConfigPath.Arena.DIAMOND_GENERATORS;
        final String childKey = nextListChildKey(yaml.getConfigurationSection(listPath), 'd', true);
        writeLocation(yaml, listPath + "." + childKey, loc);
    }

    /** Adds {@code e1}, {@code e2}, … under {@code emerald-generators}. */
    public static void appendEmeraldGenerator(final YamlConfiguration yaml, final Location loc) {
        final String listPath = ConfigPath.Arena.EMERALD_GENERATORS;
        final String childKey = nextListChildKey(yaml.getConfigurationSection(listPath), 'e', true);
        writeLocation(yaml, listPath + "." + childKey, loc);
    }

    /**
     * Next key in a numbered list: {@code d1}/{@code e1} style, or {@code iron1}/{@code gold1} under team generators.
     * When {@code acceptLegacyG} is true, existing {@code g1} keys from older wizard saves are counted too.
     */
    private static String nextListChildKey(final ConfigurationSection section, final char letter, final boolean acceptLegacyG) {
        int next = 1;
        final String prefix = String.valueOf(letter);
        if (section != null) {
            for (final String key : section.getKeys(false)) {
                final int n = suffixNumberAfterPrefix(key, prefix);
                if (n >= 0) {
                    next = Math.max(next, n + 1);
                } else if (acceptLegacyG && key.length() > 1 && key.charAt(0) == 'g') {
                    try {
                        final int legacy = Integer.parseInt(key.substring(1));
                        next = Math.max(next, legacy + 1);
                    } catch (final NumberFormatException ignored) {
                    }
                }
            }
        }
        return prefix + next;
    }

    private static int suffixNumberAfterPrefix(final String key, final String prefix) {
        if (!key.startsWith(prefix) || key.length() <= prefix.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(key.substring(prefix.length()));
        } catch (final NumberFormatException ex) {
            return -1;
        }
    }

    /** Team block keys are uppercase color names ({@code RED}, {@code BLUE}, …) like reference arenas. */
    public static void ensureTeam(final YamlConfiguration yaml, final String teamId, final String colorName) {
        final String teamKey = teamId.toUpperCase(Locale.ROOT);
        final String base = ConfigPath.Arena.TEAMS + "." + teamKey;
        yaml.set(base + "." + ConfigPath.Arena.TEAM_COLOR, colorName.toUpperCase(Locale.ROOT));
    }

    /**
     * Appends under {@code generator-iron} as {@code iron1}, {@code iron2}, … or {@code gold1}, …
     * (same naming as hand-authored arena files). Accepts legacy {@code g#} keys when picking the next index.
     */
    public static void appendTeamGenerator(final YamlConfiguration yaml,
                                           final String teamId,
                                           final String generatorKey,
                                           final Location loc) {
        final String teamKey = teamId.toUpperCase(Locale.ROOT);
        final String base = ConfigPath.Arena.TEAMS + "." + teamKey + "." + generatorKey;
        final ConfigurationSection section = yaml.getConfigurationSection(base);
        final String childPrefix = generatorKey.contains("iron") ? "iron" : "gold";
        final String childKey = nextTeamGeneratorChildKey(section, childPrefix);
        writeLocation(yaml, base + "." + childKey, loc);
    }

    private static String nextTeamGeneratorChildKey(final ConfigurationSection section, final String prefix) {
        int next = 1;
        if (section != null) {
            for (final String key : section.getKeys(false)) {
                final int n = suffixNumberAfterPrefix(key, prefix);
                if (n >= 0) {
                    next = Math.max(next, n + 1);
                } else if (key.length() > 1 && key.charAt(0) == 'g') {
                    try {
                        final int legacy = Integer.parseInt(key.substring(1));
                        next = Math.max(next, legacy + 1);
                    } catch (final NumberFormatException ignored) {
                    }
                }
            }
        }
        return prefix + next;
    }

    public static void save(final YamlConfiguration yaml, final File file) throws IOException {
        yaml.save(file);
    }
}
