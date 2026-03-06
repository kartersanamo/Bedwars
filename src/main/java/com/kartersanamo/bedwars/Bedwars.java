package com.kartersanamo.bedwars;

import com.kartersanamo.bedwars.api.IBedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.configuration.ConfigManager;
import com.kartersanamo.bedwars.arena.ArenaManager;
import com.kartersanamo.bedwars.arena.GeneratorItemTracker;
import com.kartersanamo.bedwars.arena.RejoinManager;
import com.kartersanamo.bedwars.arena.tasks.OneTickGenerators;
import com.kartersanamo.bedwars.arena.tasks.UpgradesApplyTask;
import com.kartersanamo.bedwars.commands.RejoinCommand;
import com.kartersanamo.bedwars.commands.bedwars.BedwarsCommand;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.configuration.SoundsConfig;
import com.kartersanamo.bedwars.database.Database;
import com.kartersanamo.bedwars.database.SQLite;
import com.kartersanamo.bedwars.gui.GameModeGuiListener;
import com.kartersanamo.bedwars.hologram.HologramManager;
import com.kartersanamo.bedwars.listeners.*;
import com.kartersanamo.bedwars.lobby.LobbyReturnItem;
import com.kartersanamo.bedwars.lobby.LobbyReturnListener;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import com.kartersanamo.bedwars.shop.ShopManager;
import com.kartersanamo.bedwars.shop.listeners.ShopInventoryListener;
import com.kartersanamo.bedwars.shop.listeners.ShopOpenListener;
import com.kartersanamo.bedwars.sidebar.SidebarListener;
import com.kartersanamo.bedwars.sidebar.SidebarService;
import com.kartersanamo.bedwars.sidebar.SidebarUpdateTask;
import com.kartersanamo.bedwars.upgrades.UpgradeManager;
import com.kartersanamo.bedwars.upgrades.UpgradesInventoryListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
    private RejoinManager rejoinManager;
    private InternalAdapter internalAdapter;
    private ShopManager shopManager;
    private UpgradeManager upgradeManager;
    private SidebarService sidebarService;
    private HologramManager hologramManager;

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
        this.rejoinManager = new RejoinManager();
        this.shopManager = new com.kartersanamo.bedwars.shop.ShopManager();
        this.upgradeManager = new com.kartersanamo.bedwars.upgrades.UpgradeManager();
        this.sidebarService = new com.kartersanamo.bedwars.sidebar.SidebarService(this);
        this.hologramManager = new com.kartersanamo.bedwars.hologram.HologramManager(this);

        initialiseDatabase();
        initialiseArenas();

        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopOpenListener(this, shopManager, upgradeManager), this);
        getServer().getPluginManager().registerEvents(new ShopInventoryListener(this, shopManager), this);
        getServer().getPluginManager().registerEvents(new UpgradesInventoryListener(upgradeManager), this);
        getServer().getPluginManager().registerEvents(new SidebarListener(sidebarService), this);
        getServer().getPluginManager().registerEvents(new LobbyReturnListener(this), this);
        getServer().getPluginManager().registerEvents(new GameModeGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordAndArmorEnforcementListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestDepositListener(this), this);
        getServer().getPluginManager().registerEvents(new HungerListener(this), this);
        getServer().getPluginManager().registerEvents(new RejoinListener(this), this);
        getServer().getPluginManager().registerEvents(generatorItemTracker, this);

        final BedwarsCommand bedwarsCommand = new BedwarsCommand();
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command not defined in plugin.yml")
                .setExecutor(bedwarsCommand::onCommand);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command not defined in plugin.yml")
                .setTabCompleter(bedwarsCommand::onTabComplete);

        // /rejoin command
        if (getCommand("rejoin") != null) {
            Objects.requireNonNull(getCommand("rejoin")).setExecutor(new RejoinCommand(this));
        }

        // Start global generator ticking task.
        new OneTickGenerators(this).runTaskTimer(this, 1L, 1L);

        // Start the sidebar refresh task (once per second).
        new SidebarUpdateTask(this, sidebarService).runTaskTimer(this, 20L, 20L);

        // Apply Haste and Heal Pool every 2 seconds.
        new UpgradesApplyTask(this, arenaManager).runTaskTimer(this, 40L, 40L);

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
        // Cleanly end all active games and restore maps so the next startup is fresh.
        if (arenaManager != null && internalAdapter != null) {
            final Location lobbySpawn = mainConfig != null ? mainConfig.getLobbySpawn() : null;
            for (IArena arena : arenaManager.getArenas()) {
                // Snapshot players before we start mutating arena state.
                final Collection<Player> players = new ArrayList<>(arena.getPlayers());
                final Collection<Player> spectators = new ArrayList<>(arena.getSpectators());

                handlePlayers(lobbySpawn, arena, players);
                handlePlayers(lobbySpawn, arena, spectators);

                // Restore arena blocks back to their template state and reset the internal state.
                internalAdapter.restoreArena(arena);
                arena.resetAfterGame();
            }
        }

        if (hologramManager != null) {
            hologramManager.clearAll();
        }
        if (database != null && database.isConnected()) {
            database.flushCache();
            database.disconnect();
        }

        instance = null;
    }

    private void handlePlayers(Location lobbySpawn, IArena arena, Collection<Player> players) {
        for (Player p : players) {
            if (lobbySpawn != null) {
                p.teleport(lobbySpawn);
            }
            arena.removePlayer(p, false);
            arenaManager.playerLeftArena(p);
            if (sidebarService != null) {
                sidebarService.removeSidebar(p);
            }
            LobbyReturnItem.removeFrom(p);
        }
    }

    private void initialiseDatabase() {
        if (!"sqlite".equalsIgnoreCase(mainConfig.getDatabaseType())) {
            getLogger().warning("Only SQLite is supported in this version. Falling back to SQLite.");
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
    public SidebarService getSidebarService() {
        return sidebarService;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public RejoinManager getRejoinManager() {
        return rejoinManager;
    }
}
