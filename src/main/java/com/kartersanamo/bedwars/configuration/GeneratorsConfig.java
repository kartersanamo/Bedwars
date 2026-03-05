package com.kartersanamo.bedwars.configuration;

import com.kartersanamo.bedwars.api.configuration.ConfigManager;
import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * Wrapper for {@code generators.yml}.
 */
public final class GeneratorsConfig {

    private static final String FILE_NAME = "generators.yml";

    private final FileConfiguration configuration;

    public GeneratorsConfig(final ConfigManager configManager) {
        Objects.requireNonNull(configManager, "configManager");
        configManager.saveDefaultConfigIfNotExists(FILE_NAME);
        this.configuration = configManager.getConfig(FILE_NAME);
        applyDefaults();
    }

    private void applyDefaults() {
        configuration.addDefault(ConfigPath.Generators.IRON_INTERVAL_TICKS, 20);
        configuration.addDefault(ConfigPath.Generators.IRON_MAX_ITEMS, 48);

        configuration.addDefault(ConfigPath.Generators.GOLD_INTERVAL_TICKS, 80);
        configuration.addDefault(ConfigPath.Generators.GOLD_MAX_ITEMS, 32);

        configuration.addDefault(ConfigPath.Generators.DIAMOND_INTERVAL_TICKS, 600);
        configuration.addDefault(ConfigPath.Generators.DIAMOND_MAX_ITEMS, 16);

        configuration.addDefault(ConfigPath.Generators.EMERALD_INTERVAL_TICKS, 1200);
        configuration.addDefault(ConfigPath.Generators.EMERALD_MAX_ITEMS, 16);

        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_DIAMOND_2, 300);
        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_EMERALD_2, 480);
        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_DIAMOND_3, 660);
        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_EMERALD_3, 840);
        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_BED_BREAK, 900);
        configuration.addDefault(ConfigPath.Generators.TIER_UPGRADE_SUDDEN_DEATH, 960);

        configuration.addDefault(ConfigPath.Generators.DIAMOND_TIER_2_INTERVAL_TICKS, 400);
        configuration.addDefault(ConfigPath.Generators.DIAMOND_TIER_3_INTERVAL_TICKS, 200);
        configuration.addDefault(ConfigPath.Generators.EMERALD_TIER_2_INTERVAL_TICKS, 800);
        configuration.addDefault(ConfigPath.Generators.EMERALD_TIER_3_INTERVAL_TICKS, 400);

        configuration.options().copyDefaults(true);
    }

    public int getIronIntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.IRON_INTERVAL_TICKS, 20);
    }

    public int getIronMaxItems() {
        return configuration.getInt(ConfigPath.Generators.IRON_MAX_ITEMS, 48);
    }

    public int getGoldIntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.GOLD_INTERVAL_TICKS, 80);
    }

    public int getGoldMaxItems() {
        return configuration.getInt(ConfigPath.Generators.GOLD_MAX_ITEMS, 32);
    }

    public int getDiamondIntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.DIAMOND_INTERVAL_TICKS, 600);
    }

    public int getDiamondMaxItems() {
        return configuration.getInt(ConfigPath.Generators.DIAMOND_MAX_ITEMS, 16);
    }

    public int getEmeraldIntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.EMERALD_INTERVAL_TICKS, 1200);
    }

    public int getEmeraldMaxItems() {
        return configuration.getInt(ConfigPath.Generators.EMERALD_MAX_ITEMS, 16);
    }

    public int getTierUpgradeDiamond2Seconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_DIAMOND_2, 300);
    }

    public int getTierUpgradeEmerald2Seconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_EMERALD_2, 480);
    }

    public int getTierUpgradeDiamond3Seconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_DIAMOND_3, 660);
    }

    public int getTierUpgradeEmerald3Seconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_EMERALD_3, 840);
    }

    public int getTierUpgradeBedBreakSeconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_BED_BREAK, 900);
    }

    public int getTierUpgradeSuddenDeathSeconds() {
        return configuration.getInt(ConfigPath.Generators.TIER_UPGRADE_SUDDEN_DEATH, 960);
    }

    public int getDiamondTier2IntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.DIAMOND_TIER_2_INTERVAL_TICKS, 400);
    }

    public int getDiamondTier3IntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.DIAMOND_TIER_3_INTERVAL_TICKS, 200);
    }

    public int getEmeraldTier2IntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.EMERALD_TIER_2_INTERVAL_TICKS, 800);
    }

    public int getEmeraldTier3IntervalTicks() {
        return configuration.getInt(ConfigPath.Generators.EMERALD_TIER_3_INTERVAL_TICKS, 400);
    }
}
