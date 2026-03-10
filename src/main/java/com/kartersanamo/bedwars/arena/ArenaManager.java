package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
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
    private final GeneratorItemTracker generatorItemTracker;
    private final Logger logger;

    private final Map<String, IArena> arenasById = new HashMap<>();
    private final Map<UUID, IArena> arenaByPlayer = new HashMap<>();

    private static final String INSTANCE_WORLD_ROOT_FOLDER = "bedwars_worlds";

    private static final String SHOP_NPC_ROLE = "shop";
    private static final String UPGRADES_NPC_ROLE = "upgrades";

    public ArenaManager(final JavaPlugin plugin, final MainConfig mainConfig, final GeneratorsConfig generatorsConfig, final InternalAdapter internalAdapter, final GeneratorItemTracker generatorItemTracker) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.mainConfig = Objects.requireNonNull(mainConfig, "mainConfig");
        this.generatorsConfig = Objects.requireNonNull(generatorsConfig, "generatorsConfig");
        this.internalAdapter = Objects.requireNonNull(internalAdapter, "internalAdapter");
        this.generatorItemTracker = generatorItemTracker;
        this.logger = plugin.getLogger();
    }

    public void loadArenas() {
        arenasById.clear();
        arenaByPlayer.clear();

        cleanupStaleRootInstanceWorlds();

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

            final Map<String, ArenaConfig.ModeDefinition> modeDefinitions = config.getGameModes(
                    mainConfig.getDefaultMinPlayers(),
                    mainConfig.getDefaultMaxPlayers(),
                    mainConfig.getDefaultTeamSize()
            );

            for (Map.Entry<String, ArenaConfig.ModeDefinition> modeEntry : modeDefinitions.entrySet()) {
                final String modeKey = modeEntry.getKey().toLowerCase(Locale.ROOT);
                final ArenaConfig.ModeDefinition modeDefinition = modeEntry.getValue();
                final String modeArenaId = config.getId() + "_" + modeKey;

                final World world = createOrLoadWorldInstance(config, modeKey);
                if (world == null) {
                    logger.warning("Skipping arena '" + modeArenaId + "' because its world could not be prepared.");
                    continue;
                }

                final Arena arena = Arena.fromConfig(config, modeDefinition, modeArenaId, mainConfig, plugin, world);
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
                                forwardTarget,
                                generatorItemTracker
                        ));
                    }
                    for (Location loc : teamDef.getGoldGenerators()) {
                        arena.addGenerator(new OreGenerator(
                                arena,
                                EGeneratorType.GOLD,
                                new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                                generatorsConfig.getGoldIntervalTicks(),
                                mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.GOLD),
                                forwardTarget,
                                generatorItemTracker
                        ));
                    }

                    final Location shopNpcLocation = teamDef.getShopNpc();
                    if (shopNpcLocation != null && shopNpcLocation.getWorld() != null) {
                        final Location npcLoc = new Location(world, shopNpcLocation.getX(), shopNpcLocation.getY(),
                                shopNpcLocation.getZ(), shopNpcLocation.getYaw(), shopNpcLocation.getPitch());

                        // Clean up any old armor stand holograms named "Item Shop" from templates or previous versions.
                        for (ArmorStand existing : world.getEntitiesByClass(ArmorStand.class)) {
                            if (existing.getLocation().distanceSquared(npcLoc) <= 4.0D) {
                                final String name = existing.getCustomName();
                                if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("Item Shop")) {
                                    existing.remove();
                                }
                            }
                        }

                        world.spawn(npcLoc, Villager.class, villager -> {
                            villager.setAI(false);
                            villager.setCollidable(false);
                            villager.setInvulnerable(true);
                            villager.setSilent(true);
                            villager.setCustomNameVisible(false);
                            villager.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "bw_npc_role"),
                                    PersistentDataType.STRING,
                                    SHOP_NPC_ROLE
                            );
                        });
                        final double x = npcLoc.getX();
                        final double y = npcLoc.getY();
                        final double z = npcLoc.getZ();
                        world.spawn(new Location(world, x, y + 2.25, z), ArmorStand.class, stand -> {
                            stand.setMarker(true);
                            stand.setInvisible(true);
                            stand.setGravity(false);
                            stand.setCustomNameVisible(true);
                            stand.setCustomName(ChatColor.AQUA + "ITEM SHOP");
                        });
                        world.spawn(new Location(world, x, y + 2, z), ArmorStand.class, stand -> {
                            stand.setMarker(true);
                            stand.setInvisible(true);
                            stand.setGravity(false);
                            stand.setCustomNameVisible(true);
                            stand.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + "RIGHT CLICK");
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
                            villager.setCustomNameVisible(false);
                            villager.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "bw_npc_role"),
                                    PersistentDataType.STRING,
                                    UPGRADES_NPC_ROLE
                            );
                        });
                        final double ux = upgLoc.getX();
                        final double uy = upgLoc.getY();
                        final double uz = upgLoc.getZ();
                        world.spawn(new Location(world, ux, uy + 2.0, uz), ArmorStand.class, stand -> {
                            stand.setMarker(true);
                            stand.setInvisible(true);
                            stand.setGravity(false);
                            stand.setCustomNameVisible(true);
                            stand.setCustomName(ChatColor.AQUA + "UPGRADES");
                        });
                        world.spawn(new Location(world, ux, uy + 1.75, uz), ArmorStand.class, stand -> {
                            stand.setMarker(true);
                            stand.setInvisible(true);
                            stand.setGravity(false);
                            stand.setCustomNameVisible(true);
                            stand.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + "RIGHT CLICK");
                        });
                    }
                }

                for (Location loc : config.getDiamondGenerators()) {
                    arena.addGenerator(new OreGenerator(
                            arena,
                            EGeneratorType.DIAMOND,
                            new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                            generatorsConfig.getDiamondIntervalTicks(),
                            mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.DIAMOND),
                            null,
                            generatorItemTracker
                    ));
                }

                for (Location loc : config.getEmeraldGenerators()) {
                    arena.addGenerator(new OreGenerator(
                            arena,
                            EGeneratorType.EMERALD,
                            new Location(world, loc.getX(), loc.getY(), loc.getZ()),
                            generatorsConfig.getEmeraldIntervalTicks(),
                            mainConfig.getGeneratorMaxItems(teamSize, EGeneratorType.EMERALD),
                            null,
                            generatorItemTracker
                    ));
                }

                // Pre-create "PUNCH TO / DEPOSIT" holograms above all chests and ender chests
                // inside this arena's regions so players see them without needing to punch first.
                if (plugin instanceof Bedwars bw) {
                    final var hm = bw.getHologramManager();
                    config.getArenaRegion().ifPresent(region -> spawnDepositHologramsInRegion(world, region, hm));
                    config.getLobbyRegion().ifPresent(region -> spawnDepositHologramsInRegion(world, region, hm));
                }

                arenasById.put(arena.getId().toLowerCase(Locale.ROOT), arena);
                logger.info("Loaded arena '" + arena.getId() + "' (" + arena.getDisplayName() + ", mode=" + modeKey + ")");
            }
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
     * Creates or loads an isolated world instance for the given arena config.
     * <p>
     * The template world is NEVER modified; instead we copy its folder once to
     * a new world folder and load that as the arena's active world. This allows
     * multiple arenas to share the same template map without cross-contamination.
     */
    private World createOrLoadWorldInstance(final ArenaConfig config, final String modeKey) {
        final String templateName = config.getWorldName();
        if (templateName == null || templateName.isEmpty()) {
            logger.warning("Arena '" + config.getId() + "' has no world name configured.");
            return null;
        }

        final String safeMode = modeKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        final String instanceLeaf = templateName + "_bw_" + config.getId() + "_" + safeMode;
        final String instanceName = INSTANCE_WORLD_ROOT_FOLDER + "/" + instanceLeaf;

        // If the instance world is already loaded, reuse it.
        World existing = Bukkit.getWorld(instanceName);
        if (existing != null) {
            return existing;
        }

        final File worldContainer = Bukkit.getWorldContainer();
        final Path templatePath = worldContainer.toPath().resolve(templateName);
        final Path instancesRoot = worldContainer.toPath().resolve(INSTANCE_WORLD_ROOT_FOLDER);
        final Path instancePath = instancesRoot.resolve(instanceLeaf);

        if (!Files.isDirectory(templatePath)) {
            logger.warning("Template world folder '" + templatePath + "' for arena '" + config.getId() + "' does not exist.");
            return null;
        }

        try {
            if (!Files.isDirectory(instancesRoot)) {
                Files.createDirectories(instancesRoot);
            }
            if (Files.isDirectory(instancePath)) {
                deleteWorldFolder(instancePath);
            }
            copyWorldFolder(templatePath, instancePath);
        } catch (IOException ex) {
            logger.severe("Failed to prepare world '" + templateName + "' for arena '" + config.getId() + "' mode '" + modeKey + "': " + ex.getMessage());
            return null;
        }

        final WorldCreator creator = new WorldCreator(instanceName);
        creator.environment(Objects.requireNonNullElse(Bukkit.getWorld(templateName), Bukkit.getWorlds().getFirst()).getEnvironment());
        return Bukkit.createWorld(creator);
    }

    /**
     * On startup, scans the server root for old-style Bedwars instance world folders
     * (matching {@code *_bw_*}) that were created before worlds were moved under
     * {@value INSTANCE_WORLD_ROOT_FOLDER}. Any that are not currently loaded by Bukkit
     * are deleted automatically so the root folder stays clean.
     */
    private void cleanupStaleRootInstanceWorlds() {
        final File worldContainer = Bukkit.getWorldContainer();
        final File[] entries = worldContainer.listFiles();
        if (entries == null) {
            return;
        }

        int deleted = 0;
        int skipped = 0;

        for (final File entry : entries) {
            if (!entry.isDirectory()) {
                continue;
            }
            final String name = entry.getName();
            // Only touch folders that match the old naming pattern.
            if (!name.contains("_bw_")) {
                continue;
            }
            // Skip if this is our new organised root folder itself.
            if (name.equals(INSTANCE_WORLD_ROOT_FOLDER)) {
                continue;
            }
            // Safety: never touch a world that is currently loaded in Bukkit.
            if (Bukkit.getWorld(name) != null) {
                logger.warning("[BW Cleanup] Stale root instance folder '" + name
                        + "' is still loaded — skipping. Unload it and restart to clean up.");
                skipped++;
                continue;
            }
            // Delete the stale folder.
            try {
                deleteWorldFolder(entry.toPath());
                logger.info("[BW Cleanup] Deleted stale root instance world: " + name);
                deleted++;
            } catch (IOException ex) {
                logger.warning("[BW Cleanup] Failed to delete stale world '" + name + "': " + ex.getMessage());
            }
        }

        if (deleted > 0 || skipped > 0) {
            logger.info("[BW Cleanup] Root cleanup complete — deleted: " + deleted + ", skipped (still loaded): " + skipped);
        }
    }

    private void spawnDepositHologramsInRegion(final World world,
                                               final ArenaConfig.Region region,
                                               final com.kartersanamo.bedwars.hologram.HologramManager hologramManager) {
        if (world == null || region == null || hologramManager == null) {
            return;
        }
        final Location pos1 = region.getPos1();
        final Location pos2 = region.getPos2();
        if (pos1 == null || pos2 == null) {
            return;
        }

        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final BlockState state = block.getState();
                    if (state instanceof Chest || block.getType() == org.bukkit.Material.ENDER_CHEST) {
                        hologramManager.ensureDepositHologram(block.getLocation());
                    }
                }
            }
        }
    }

    @SuppressWarnings("resource")
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

    @SuppressWarnings("resource")
    private void deleteWorldFolder(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        // Delete children first, then the directory itself.
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }
}
