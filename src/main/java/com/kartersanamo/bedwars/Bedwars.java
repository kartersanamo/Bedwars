package com.kartersanamo.bedwars;

import com.kartersanamo.bedwars.api.IBedwars;
import com.kartersanamo.bedwars.api.configuration.ConfigManager;
import com.kartersanamo.bedwars.arena.ArenaManager;
import com.kartersanamo.bedwars.arena.GeneratorItemTracker;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.configuration.SoundsConfig;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.database.SQLite;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class Bedwars extends JavaPlugin implements IBedwars {

    private static Bedwars instance;

    private ConfigManager configManager;
    private MainConfig mainConfig;
    private GeneratorsConfig generatorsConfig;
    private SoundsConfig soundsConfig;

    private Database database;
    private ArenaManager arenaManager;
    private GeneratorItemTracker generatorItemTracker;
    private InternalAdapter internalAdapter;
    private com.kartersanamo.bedwars.shop.ShopManager shopManager;
    private com.kartersanamo.bedwars.upgrades.UpgradeManager upgradeManager;
    private com.kartersanamo.bedwars.sidebar.SidebarService sidebarService;
    private com.kartersanamo.bedwars.hologram.HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        saveDefaultConfig();

        this.mainConfig = new MainConfig(configManager, getLogger());
        this.generatorsConfig = new GeneratorsConfig(configManager);
        this.soundsConfig = new SoundsConfig(configManager);

        this.internalAdapter = new InternalAdapter();
        this.generatorItemTracker = new GeneratorItemTracker(this);
        this.shopManager = new com.kartersanamo.bedwars.shop.ShopManager();
        this.upgradeManager = new com.kartersanamo.bedwars.upgrades.UpgradeManager();
        this.sidebarService = new com.kartersanamo.bedwars.sidebar.SidebarService(this);
        this.hologramManager = new com.kartersanamo.bedwars.hologram.HologramManager(this);

        initialiseDatabase();
        initialiseArenas();

        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.shop.listeners.ShopOpenListener(this, shopManager, upgradeManager), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.shop.listeners.ShopInventoryListener(this, shopManager), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.upgrades.UpgradesInventoryListener(upgradeManager), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.sidebar.SidebarListener(sidebarService), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.gui.GameModeGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.SwordAndArmorEnforcementListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kartersanamo.bedwars.listeners.HungerListener(this), this);
        getServer().getPluginManager().registerEvents(generatorItemTracker, this);

        final com.kartersanamo.bedwars.commands.bedwars.BedwarsCommand bedwarsCommand =
                new com.kartersanamo.bedwars.commands.bedwars.BedwarsCommand();
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command not defined in plugin.yml")
                .setExecutor(bedwarsCommand::onCommand);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command not defined in plugin.yml")
                .setTabCompleter(bedwarsCommand::onTabComplete);

        // Start global generator ticking task.
        new com.kartersanamo.bedwars.arena.tasks.OneTickGenerators(this)
                .runTaskTimer(this, 1L, 1L);

        // Start sidebar refresh task (once per second).
        new com.kartersanamo.bedwars.sidebar.SidebarUpdateTask(this, sidebarService)
                .runTaskTimer(this, 20L, 20L);

        // Apply Haste and Heal Pool every 2 seconds.
        new com.kartersanamo.bedwars.arena.tasks.UpgradesApplyTask(this, arenaManager)
                .runTaskTimer(this, 40L, 40L);

        // Clean up generator item tracker for merged/despawned items (every 5 seconds).
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (generatorItemTracker != null) {
                    generatorItemTracker.cleanupRemovedEntities();
                }
            }
        }.runTaskTimer(this, 100L, 100L);

    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.clearAll();
        }
        if (database != null && database.isConnected()) {
            database.flushCache();
            database.disconnect();
        }

        instance = null;
    }

    private void initialiseDatabase() {
        if (!"sqlite".equalsIgnoreCase(mainConfig.getDatabaseType())) {
            getLogger().warning("Only SQLite is supported in the MVP. Falling back to SQLite.");
        }

        this.database = new SQLite(this, mainConfig.getSqliteFileName());
        try {
            database.connect();
            database.initSchema();
        } catch (SQLException ex) {
            getLogger().severe("Failed to initialise SQLite database: " + ex.getMessage());
        }
    }

    private void initialiseArenas() {
        this.arenaManager = new ArenaManager(this, mainConfig, generatorsConfig, internalAdapter, generatorItemTracker);
        arenaManager.loadArenas();

        if (arenaManager.getArenas().isEmpty()) {
            getLogger().warning("No Bedwars arenas are currently configured. " +
                    "Create configuration files under " + getDataFolder() + "/arenas");
        }
    }

    public static Bedwars getInstance() {
        return instance;
    }

    // IBedwars API implementation

    @Override
    public Bedwars getPlugin() {
        return this;
    }

    @Override
    public MainConfig getMainConfig() {
        return mainConfig;
    }

    @Override
    public GeneratorsConfig getGeneratorsConfig() {
        return generatorsConfig;
    }

    @Override
    public SoundsConfig getSoundsConfig() {
        return soundsConfig;
    }

    @Override
    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public InternalAdapter getInternalAdapter() {
        return internalAdapter;
    }

    @Override
    public com.kartersanamo.bedwars.sidebar.SidebarService getSidebarService() {
        return sidebarService;
    }

    public com.kartersanamo.bedwars.hologram.HologramManager getHologramManager() {
        return hologramManager;
    }
}
