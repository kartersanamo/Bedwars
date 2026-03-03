package com.kartersanamo.bedwars.api.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Central helper for working with plugin configuration files.
 *
 * This class intentionally keeps its surface area small and focused:
 * - ensure that default resources are copied to the data folder
 * - load and cache {@link FileConfiguration} instances by file name
 * - provide explicit reload capability
 *
 * Strongly-typed wrappers such as {@code MainConfig} or {@code ArenaConfig}
 * should depend on this class instead of duplicating file management logic.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> cachedConfigs = new HashMap<>();

    public ConfigManager(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns a configuration for the given file name, creating and loading it if necessary.
     * <p>
     * The {@code fileName} is relative to the plugin data folder, for example {@code "config.yml"}
     * or {@code "generators.yml"}.
     */
    public FileConfiguration getConfig(final String fileName) {
        Objects.requireNonNull(fileName, "fileName");

        if ("config.yml".equalsIgnoreCase(fileName)) {
            // Delegate to the built-in config for the primary file.
            return plugin.getConfig();
        }

        return cachedConfigs.computeIfAbsent(fileName, this::loadConfigInternal);
    }

    /**
     * Reloads the configuration for the given file name.
     */
    public FileConfiguration reloadConfig(final String fileName) {
        Objects.requireNonNull(fileName, "fileName");

        if ("config.yml".equalsIgnoreCase(fileName)) {
            plugin.reloadConfig();
            return plugin.getConfig();
        }

        final FileConfiguration configuration = loadConfigInternal(fileName);
        cachedConfigs.put(fileName, configuration);
        return configuration;
    }

    /**
     * Ensures that a default configuration file exists in the data folder by copying it
     * from the plugin JAR if it is missing.
     *
     * If the resource does not exist in the JAR this method does nothing.
     */
    public void saveDefaultConfigIfNotExists(final String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");

        final File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (outFile.exists()) {
            return;
        }

        if (plugin.getResource(resourcePath) == null) {
            // No bundled default; nothing to copy.
            return;
        }

        plugin.saveResource(resourcePath, false);
    }

    /**
     * Saves the given configuration back to disk.
     */
    public void saveConfig(final String fileName) throws IOException {
        Objects.requireNonNull(fileName, "fileName");

        if ("config.yml".equalsIgnoreCase(fileName)) {
            plugin.saveConfig();
            return;
        }

        final File file = new File(plugin.getDataFolder(), fileName);
        final FileConfiguration configuration = cachedConfigs.getOrDefault(fileName, loadConfigInternal(fileName));
        configuration.save(file);
    }

    /**
     * Returns an unmodifiable view of the currently cached configurations.
     */
    public Map<String, FileConfiguration> getCachedConfigs() {
        return Collections.unmodifiableMap(cachedConfigs);
    }

    private FileConfiguration loadConfigInternal(final String fileName) {
        final File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            // Try to copy a default resource if present.
            saveDefaultConfigIfNotExists(fileName);
        }

        final FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        // Load defaults from the JAR, if available, so that missing values
        // fall back to the bundled configuration.
        final InputStream defaultsStream = plugin.getResource(fileName);
        if (defaultsStream != null) {
            final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)
            );
            configuration.setDefaults(defaults);
            configuration.options().copyDefaults(true);
        }

        return configuration;
    }
}
