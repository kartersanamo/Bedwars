package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;

/**
 * Central registry for all arenas on this server instance.
 */
public final class ArenaManager {

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final GeneratorsConfig generatorsConfig;
    private final InternalAdapter internalAdapter;
    private final Logger logger;

    private final Map<String, IArena> arenasById = new HashMap<>();
    private final Map<UUID, IArena> arenaByPlayer = new HashMap<>();

    public ArenaManager(final JavaPlugin plugin, final MainConfig mainConfig, final GeneratorsConfig generatorsConfig, final InternalAdapter internalAdapter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.mainConfig = Objects.requireNonNull(mainConfig, "mainConfig");
        this.generatorsConfig = Objects.requireNonNull(generatorsConfig, "generatorsConfig");
        this.internalAdapter = Objects.requireNonNull(internalAdapter, "internalAdapter");
        this.logger = plugin.getLogger();
    }

    public void loadArenas() {
        arenasById.clear();
        arenaByPlayer.clear();

        final File arenasDirectory = new File(plugin.getDataFolder(), "arenas");
        final List<ArenaConfig> configs = ArenaConfig.loadAll(arenasDirectory, logger);
        if (configs.isEmpty()) {
            logger.warning("No arena configuration files found in " + arenasDirectory.getAbsolutePath());
            return;
        }

        for (ArenaConfig config : configs) {
            if (!config.isEnabled()) {
                continue;
            }

            final World world = createOrLoadWorldInstance(config);
            if (world == null) {
                logger.warning("Skipping arena '" + config.getId() + "' because its world could not be prepared.");
                continue;
            }

            final Arena arena = Arena.fromConfig(config, mainConfig, plugin, world);
            if (!arena.validate()) {
                logger.warning("Disabling arena '" + arena.getId() + "' due to validation errors.");
                arena.setEnabled(false);
                continue;
            }

            // Snapshot both arena-region and lobby-region (if present) so that
            // both the playable map and the waiting lobby can be restored to
            // their original state between games.
            final Optional<ArenaConfig.Region> arenaRegionOpt = config.getArenaRegion();
            final Optional<ArenaConfig.Region> lobbyRegionOpt = config.getLobbyRegion();
            if (arenaRegionOpt.isPresent()) {
                final ArenaConfig.Region base = arenaRegionOpt.get();
                final ArenaConfig.Region snapshotRegion;
                if (lobbyRegionOpt.isPresent()) {
                    final Location a1 = base.getPos1();
                    final Location a2 = base.getPos2();
                    final Location b1 = lobbyRegionOpt.get().getPos1();
                    final Location b2 = lobbyRegionOpt.get().getPos2();

                    final int minX = Math.min(Math.min(a1.getBlockX(), a2.getBlockX()), Math.min(b1.getBlockX(), b2.getBlockX()));
                    final int maxX = Math.max(Math.max(a1.getBlockX(), a2.getBlockX()), Math.max(b1.getBlockX(), b2.getBlockX()));
                    final int minY = Math.min(Math.min(a1.getBlockY(), a2.getBlockY()), Math.min(b1.getBlockY(), b2.getBlockY()));
                    final int maxY = Math.max(Math.max(a1.getBlockY(), a2.getBlockY()), Math.max(b1.getBlockY(), b2.getBlockY()));
                    final int minZ = Math.min(Math.min(a1.getBlockZ(), a2.getBlockZ()), Math.min(b1.getBlockZ(), b2.getBlockZ()));
                    final int maxZ = Math.max(Math.max(a1.getBlockZ(), a2.getBlockZ()), Math.max(b1.getBlockZ(), b2.getBlockZ()));

                    final Location pos1 = new Location(world, minX, minY, minZ);
                    final Location pos2 = new Location(world, maxX, maxY, maxZ);
                    snapshotRegion = new ArenaConfig.Region(pos1, pos2);
                } else {
                    snapshotRegion = base;
                }
                internalAdapter.snapshotArena(arena, snapshotRegion);
            }

            final int teamSize = arena.getTeamSize();

            // Create generators and spawn shop NPCs from configuration.
            for (ArenaConfig.TeamDefinition teamDef : config.getTeamDefinitions()) {
                final Location forwardTarget = new Location(world,
                        teamDef.getSpawn().getX(), teamDef.getSpawn().getY(), teamDef.getSpawn().getZ());
                for (Location loc : teamDef.getIronGenerators()) {
                    arena.addGenerator(new OreGenerator(
                            arena,
                            EGeneratorType.IRON,
                            new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                            generatorsConfig.getIronIntervalTicks(),
                            mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.IRON),
                            forwardTarget
                    ));
                }
                for (Location loc : teamDef.getGoldGenerators()) {
                    arena.addGenerator(new OreGenerator(
                            arena,
                            EGeneratorType.GOLD,
                            new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                            generatorsConfig.getGoldIntervalTicks(),
                            mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.GOLD),
                            forwardTarget
                    ));
                }

                final Location shopNpcLocation = teamDef.getShopNpc();
                if (shopNpcLocation != null && shopNpcLocation.getWorld() != null) {
                    final Location npcLoc = new Location(world, shopNpcLocation.getX(), shopNpcLocation.getY(),
                            shopNpcLocation.getZ(), shopNpcLocation.getYaw(), shopNpcLocation.getPitch());
                    world.spawn(npcLoc, Villager.class, villager -> {
                        villager.setAI(false);
                        villager.setCollidable(false);
                        villager.setInvulnerable(true);
                        villager.setSilent(true);
                        villager.setCustomNameVisible(true);
                        villager.setCustomName(teamDef.getColor().getChatColor() + "Item Shop");
                    });
                    final double x = npcLoc.getX();
                    final double y = npcLoc.getY();
                    final double z = npcLoc.getZ();
                    world.spawn(new Location(world, x, y + 2.0, z), ArmorStand.class, stand -> {
                        stand.setMarker(true);
                        stand.setInvisible(true);
                        stand.setGravity(false);
                        stand.setCustomNameVisible(true);
                        stand.setCustomName(ChatColor.GREEN + "ITEM SHOP");
                    });
                    world.spawn(new Location(world, x, y + 1.75, z), ArmorStand.class, stand -> {
                        stand.setMarker(true);
                        stand.setInvisible(true);
                        stand.setGravity(false);
                        stand.setCustomNameVisible(true);
                        stand.setCustomName(ChatColor.GOLD + "RIGHT CLICK");
                    });
                }

                final Location upgradeNpcLocation = teamDef.getUpgradeNpc();
                if (upgradeNpcLocation != null && upgradeNpcLocation.getWorld() != null) {
                    final Location upgLoc = new Location(world, upgradeNpcLocation.getX(), upgradeNpcLocation.getY(),
                            upgradeNpcLocation.getZ(), upgradeNpcLocation.getYaw(), upgradeNpcLocation.getPitch());
                    world.spawn(upgLoc, Villager.class, villager -> {
                        villager.setAI(false);
                        villager.setCollidable(false);
                        villager.setInvulnerable(true);
                        villager.setSilent(true);
                        villager.setCustomNameVisible(true);
                        villager.setCustomName(teamDef.getColor().getChatColor() + "Upgrades");
                    });
                    final double ux = upgLoc.getX();
                    final double uy = upgLoc.getY();
                    final double uz = upgLoc.getZ();
                    world.spawn(new Location(world, ux, uy + 2.0, uz), ArmorStand.class, stand -> {
                        stand.setMarker(true);
                        stand.setInvisible(true);
                        stand.setGravity(false);
                        stand.setCustomNameVisible(true);
                        stand.setCustomName(ChatColor.GREEN + "UPGRADES");
                    });
                    world.spawn(new Location(world, ux, uy + 1.75, uz), ArmorStand.class, stand -> {
                        stand.setMarker(true);
                        stand.setInvisible(true);
                        stand.setGravity(false);
                        stand.setCustomNameVisible(true);
                        stand.setCustomName(ChatColor.GOLD + "RIGHT CLICK");
                    });
                }
            }

            for (Location loc : config.getDiamondGenerators()) {
                arena.addGenerator(new OreGenerator(
                        arena,
                        EGeneratorType.DIAMOND,
                        new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                        generatorsConfig.getDiamondIntervalTicks(),
                        mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.DIAMOND)
                ));
            }

            for (Location loc : config.getEmeraldGenerators()) {
                arena.addGenerator(new OreGenerator(
                        arena,
                        EGeneratorType.EMERALD,
                        new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                        generatorsConfig.getEmeraldIntervalTicks(),
                        mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.EMERALD)
                ));
            }

            arenasById.put(arena.getId().toLowerCase(Locale.ROOT), arena);
            logger.info("Loaded arena '" + arena.getId() + "' (" + arena.getDisplayName() + ")");
        }
    }

    public Collection<IArena> getArenas() {
        return Collections.unmodifiableCollection(arenasById.values());
    }

    public IArena getArena(final String id) {
        return id == null ? null : arenasById.get(id.toLowerCase(Locale.ROOT));
    }

    public IArena getArena(final Player player) {
        return arenaByPlayer.get(player.getUniqueId());
    }

    public void playerJoinedArena(final Player player, final IArena arena) {
        arenaByPlayer.put(player.getUniqueId(), arena);
    }

    public void playerLeftArena(final Player player) {
        arenaByPlayer.remove(player.getUniqueId());
    }

    /**
     * Finds the best joinable arena for the given player.
     *
     * Preference is given to arenas that are already filling up but not yet full.
     */
    public Optional<IArena> findBestJoinableArena() {
        IArena best = null;
        int bestPlayerCount = -1;

        for (IArena arena : arenasById.values()) {
            if (!arena.isEnabled()) {
                continue;
            }
            final EGameState state = arena.getGameState();
            if (state != EGameState.LOBBY_WAITING && state != EGameState.STARTING) {
                continue;
            }
            final int size = arena.getPlayers().size();
            if (size >= arena.getMaxPlayers()) {
                continue;
            }

            if (size > bestPlayerCount) {
                bestPlayerCount = size;
                best = arena;
            }
        }

        return Optional.ofNullable(best);
    }

    /**
     * Creates or loads an isolated world instance for the given arena config.
     *
     * The template world is NEVER modified; instead we copy its folder once to
     * a new world folder and load that as the arena's active world. This allows
     * multiple arenas to share the same template map without cross-contamination.
     */
    private World createOrLoadWorldInstance(final ArenaConfig config) {
        final String templateName = config.getWorldName();
        if (templateName == null || templateName.isEmpty()) {
            logger.warning("Arena '" + config.getId() + "' has no world name configured.");
            return null;
        }

        final String instanceName = templateName + "_bw_" + config.getId();

        // If the instance world is already loaded, reuse it.
        World existing = Bukkit.getWorld(instanceName);
        if (existing != null) {
            return existing;
        }

        final File worldContainer = Bukkit.getWorldContainer();
        final Path templatePath = worldContainer.toPath().resolve(templateName);
        final Path instancePath = worldContainer.toPath().resolve(instanceName);

        if (!Files.isDirectory(templatePath)) {
            logger.warning("Template world folder '" + templatePath + "' for arena '" + config.getId() + "' does not exist.");
            return null;
        }

        // Only copy if the instance folder does not already exist (e.g. after server restart).
        if (!Files.isDirectory(instancePath)) {
            try {
                copyWorldFolder(templatePath, instancePath);
            } catch (IOException ex) {
                logger.severe("Failed to copy world '" + templateName + "' for arena '" + config.getId() + "': " + ex.getMessage());
                return null;
            }
        }

        final WorldCreator creator = new WorldCreator(instanceName);
        creator.environment(Objects.requireNonNullElse(Bukkit.getWorld(templateName), Bukkit.getWorlds().get(0)).getEnvironment());
        return Bukkit.createWorld(creator);
    }

    private void copyWorldFolder(final Path source, final Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                final Path relative = source.relativize(path);
                final Path dest = target.resolve(relative);

                if (Files.isDirectory(path)) {
                    if (!Files.isDirectory(dest)) {
                        Files.createDirectories(dest);
                    }
                } else {
                    final String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    // Skip files that should not be cloned verbatim.
                    if ("session.lock".equals(name) || "uid.dat".equals(name)) {
                        return;
                    }
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
