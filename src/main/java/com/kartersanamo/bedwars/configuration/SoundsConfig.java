package com.kartersanamo.bedwars.configuration;

import com.kartersanamo.bedwars.api.configuration.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * Wrapper for {@code sounds.yml}.
 */
public final class SoundsConfig {

    private static final String FILE_NAME = "sounds.yml";

    private final FileConfiguration configuration;

    public SoundsConfig(final ConfigManager configManager) {
        Objects.requireNonNull(configManager, "configManager");
        configManager.saveDefaultConfigIfNotExists(FILE_NAME);
        this.configuration = configManager.getConfig(FILE_NAME);
        applyDefaults();
    }

    private void applyDefaults() {
        addDefaultSound("bed-destroyed", Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
        addDefaultSound("kill", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        addDefaultSound("final-kill", Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        addDefaultSound("countdown-tick", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        addDefaultSound("game-start", Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        addDefaultSound("victory", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        addDefaultSound("defeat", Sound.BLOCK_ANVIL_LAND, 0.8f, 0.8f);
        addDefaultSound("generator-pickup", Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.3f);

        configuration.options().copyDefaults(true);
    }

    private void addDefaultSound(final String key, final Sound sound, final float volume, final float pitch) {
        final String path = key;
        configuration.addDefault(path + ".sound", sound.name());
        configuration.addDefault(path + ".volume", volume);
        configuration.addDefault(path + ".pitch", pitch);
    }

    public SoundSettings getSound(final String key) {
        final ConfigurationSection section = configuration.getConfigurationSection(key);
        if (section == null) {
            return null;
        }

        final String soundName = section.getString("sound");
        if (soundName == null) {
            return null;
        }

        final Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        final float volume = (float) section.getDouble("volume", 1.0);
        final float pitch = (float) section.getDouble("pitch", 1.0);
        return new SoundSettings(sound, volume, pitch);
    }

    public static final class SoundSettings {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundSettings(final Sound sound, final float volume, final float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
