package com.kartersanamo.bedwars.api;

import com.kartersanamo.bedwars.arena.ArenaManager;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.configuration.SoundsConfig;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import com.kartersanamo.bedwars.sidebar.SidebarService;
import org.bukkit.plugin.Plugin;

/**
 * Public-facing Bedwars API exposed by the plugin main class.
 *
 * This allows other components (and potentially other plugins) to access
 * core services without depending directly on the concrete implementation.
 */
public interface IBedwars {

    /**
     * Returns the underlying Bukkit plugin instance.
     */
    Plugin getPlugin();

    /**
     * Global configuration wrapper.
     */
    MainConfig getMainConfig();

    /**
     * Generator timing configuration wrapper.
     */
    GeneratorsConfig getGeneratorsConfig();

    /**
     * Sound configuration wrapper.
     */
    SoundsConfig getSoundsConfig();

    /**
     * Arena manager that owns all loaded arenas.
     */
    ArenaManager getArenaManager();

    /**
     * Database facade used for player statistics.
     */
    Database getDatabase();

    /**
     * Internal map restore adapter used by arenas and listeners.
     */
    InternalAdapter getInternalAdapter();

    /**
     * Sidebar manager for in-game scoreboards. Remove a player's sidebar when they leave an arena.
     */
    SidebarService getSidebarService();
}
