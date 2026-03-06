package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import com.kartersanamo.bedwars.arena.tasks.GamePlayingTask;
import com.kartersanamo.bedwars.arena.tasks.GameRestartingTask;
import com.kartersanamo.bedwars.arena.tasks.GameStartingTask;
import com.kartersanamo.bedwars.arena.team.BedwarsTeam;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import com.kartersanamo.bedwars.upgrades.TeamUpgradeState;
import com.kartersanamo.bedwars.upgrades.TrapType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Concrete arena implementation.
 * <p>
 * A large part of the game logic is orchestrated by task classes in
 * {@code arena.tasks}, but the arena itself owns the high-level state and
 * participant tracking.
 */
public final class Arena implements IArena {

    private final String id;
    private final String displayName;
    private final World world;
    private final Location lobbySpawn;
    private final Location spectatorSpawn;
    private final int minPlayers;
    private final int maxPlayers;
    private final int teamSize;

    private final ArenaConfig.Region region;
    private final ArenaConfig.Region lobbyRegion;

    private final List<ITeam> teams;
    private final List<IGenerator> generators = new ArrayList<>();

    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();

    /** Expiry timestamp (system millis) for spawn protection per player. */
    private final Map<UUID, Long> spawnProtectionUntil = new HashMap<>();

    private final Map<UUID, ArmorTier> playerArmorTier = new HashMap<>();
    private final Map<UUID, ToolTier> playerAxeTier = new HashMap<>();
    private final Map<UUID, ToolTier> playerPickaxeTier = new HashMap<>();
    private final Set<UUID> playerHasShears = new HashSet<>();

    /** Kill count this game per player (for end-of-game summary). */
    private final Map<UUID, Integer> killCountThisGame = new HashMap<>();
    /** Final kills this game per player (for tab list). */
    private final Map<UUID, Integer> finalKillsThisGame = new HashMap<>();
    /** Beds broken this game per player (for tab list). */
    private final Map<UUID, Integer> bedsBrokenThisGame = new HashMap<>();

    /** Per-team diamond upgrade and trap state. */
    private final Map<String, TeamUpgradeState> teamUpgradeStates = new HashMap<>();

    /** Active dragons spawned for Sudden Death per team (teamId -> dragon UUIDs). */
    private final Map<String, List<UUID>> teamDragonIds = new HashMap<>();

    /** Players currently inside an enemy base (teamId -> set of player UUIDs) for trap trigger cooldown. */
    private final Map<String, Set<UUID>> playersInsideEnemyBase = new HashMap<>();

    private static final int BASE_TRAP_RADIUS = 10;
    private static final int HEAL_POOL_RADIUS = 12;

    private final JavaPlugin plugin;

    private boolean enabled = true;
    private EGameState gameState = EGameState.LOBBY_WAITING;

    private GameStartingTask startingTask;
    private GamePlayingTask playingTask;

    /** When the game started (IN_GAME); used for tier upgrade timers. */
    private long gameStartMillis;
    private int diamondTier = 1;
    private int emeraldTier = 1;
    private GeneratorPhase generatorPhase = GeneratorPhase.NORMAL;
    private boolean suddenDeathStarted;
    private boolean gameOverWarningSent;

    private Arena(final String id,
                  final String displayName,
                  final World world,
                  final Location lobbySpawn,
                  final Location spectatorSpawn,
                  final int minPlayers,
                  final int maxPlayers,
                  final int teamSize,
                  final ArenaConfig.Region region,
                  final ArenaConfig.Region lobbyRegion,
                  final List<ITeam> teams,
                  final JavaPlugin plugin) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.lobbySpawn = lobbySpawn;
        this.spectatorSpawn = spectatorSpawn;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teamSize = teamSize;
        this.region = region;
        this.lobbyRegion = lobbyRegion;
        this.teams = Collections.unmodifiableList(new ArrayList<>(teams));
        this.plugin = plugin;
        for (ITeam t : teams) {
            this.teamUpgradeStates.put(t.getId(), new TeamUpgradeState());
            this.teamDragonIds.put(t.getId(), new ArrayList<>());
        }
    }

    public static Arena fromConfig(final ArenaConfig config,
                                   final MainConfig mainConfig,
                                   final JavaPlugin plugin,
                                   final World world) {
        final String id = config.getId();

        // Rebind all locations to the per-arena instance world.
        final Location rawLobby = config.getLobbySpawn();
        final Location lobbySpawn = rawLobby != null
                ? new Location(world, rawLobby.getX(), rawLobby.getY(), rawLobby.getZ(),
                rawLobby.getYaw(), rawLobby.getPitch())
                : world.getSpawnLocation();

        final Location rawSpectator = config.getSpectatorSpawn();
        final Location spectatorSpawn = rawSpectator != null
                ? new Location(world, rawSpectator.getX(), rawSpectator.getY(), rawSpectator.getZ(),
                rawSpectator.getYaw(), rawSpectator.getPitch())
                : lobbySpawn;

        final int minPlayers = config.getMinPlayers(mainConfig.getDefaultMinPlayers());
        final int maxPlayers = config.getMaxPlayers(mainConfig.getDefaultMaxPlayers());
        final int teamSize = config.getTeamSize(mainConfig.getDefaultTeamSize());

        final ArenaConfig.Region region = config.getArenaRegion()
                .map(r -> new ArenaConfig.Region(
                        new Location(world, r.getPos1().getX(), r.getPos1().getY(), r.getPos1().getZ()),
                        new Location(world, r.getPos2().getX(), r.getPos2().getY(), r.getPos2().getZ())
                ))
                .orElse(null);

        final ArenaConfig.Region lobbyRegion = config.getLobbyRegion()
                .map(r -> new ArenaConfig.Region(
                        new Location(world, r.getPos1().getX(), r.getPos1().getY(), r.getPos1().getZ()),
                        new Location(world, r.getPos2().getX(), r.getPos2().getY(), r.getPos2().getZ())
                ))
                .orElse(null);

        final List<ITeam> teams = new ArrayList<>();
        for (ArenaConfig.TeamDefinition def : config.getTeamDefinitions()) {
            final Location rawSpawn = def.getSpawn();
            final Location spawn = new Location(world, rawSpawn.getX(), rawSpawn.getY(), rawSpawn.getZ(),
                    rawSpawn.getYaw(), rawSpawn.getPitch());

            final Location rawBed = def.getBed();
            final Location bed = new Location(world, rawBed.getX(), rawBed.getY(), rawBed.getZ(),
                    rawBed.getYaw(), rawBed.getPitch());

            // Ensure the physical bed block matches the team color (e.g. RED_BED for red team).
            applyTeamBedColor(world, bed, def.getColor());

            teams.add(new BedwarsTeam(def.getId(), def.getColor(), spawn, bed, plugin));
        }

        return new Arena(
                id,
                config.getDisplayName(),
                world,
                lobbySpawn,
                spectatorSpawn,
                minPlayers,
                maxPlayers,
                teamSize,
                region,
                lobbyRegion,
                teams,
                plugin
        );
    }

    /**
     * Performs validation of the loaded configuration, returning {@code true}
     * if the arena is considered safe to use.
     */
    public boolean validate() {
        if (world == null) {
            plugin.getLogger().warning("Arena '" + id + "' has no valid world loaded.");
            return false;
        }

        if (teams.size() < 2) {
            plugin.getLogger().warning("Arena '" + id + "' has fewer than two teams configured.");
            return false;
        }

        return true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public EGameState getGameState() {
        return gameState;
    }

    @Override
    public void setGameState(final EGameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getMinPlayers() {
        return minPlayers;
    }

    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public int getTeamSize() {
        return teamSize;
    }

    @Override
    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    @Override
    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public Optional<ArenaConfig.Region> getLobbyRegion() {
        return Optional.ofNullable(lobbyRegion);
    }

    @Override
    public List<ITeam> getTeams() {
        return teams;
    }

    public TeamUpgradeState getTeamUpgradeState(final ITeam team) {
        return team == null ? null : teamUpgradeStates.get(team.getId());
    }

    @Override
    public Optional<ITeam> getTeam(final Player player) {
        return teams.stream()
                .filter(team -> team.contains(player))
                .findFirst();
    }

    @Override
    public Collection<Player> getPlayers() {
        final List<Player> result = new ArrayList<>();
        for (UUID uuid : players) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    @Override
    public Collection<Player> getSpectators() {
        final List<Player> result = new ArrayList<>();
        for (UUID uuid : spectators) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    @Override
    public List<IGenerator> getGenerators() {
        return Collections.unmodifiableList(generators);
    }

    public void addGenerator(final IGenerator generator) {
        this.generators.add(generator);
    }

    /**
     * Returns whether the given location is inside the configured arena region.
     */
    public boolean isInsideRegion(final Location location) {
        if (region == null || location == null || !Objects.equals(location.getWorld(), world)) {
            return false;
        }

        final Location pos1 = region.getPos1();
        final Location pos2 = region.getPos2();

        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    @Override
    public boolean addPlayer(final Player player) {
        if (!enabled) {
            return false;
        }

        if (players.contains(player.getUniqueId())) {
            return true;
        }

        if (players.size() >= maxPlayers) {
            return false;
        }

        players.add(player.getUniqueId());
        spectators.remove(player.getUniqueId());

        // Team assignment will be handled by TeamAssigner later in the flow.
        player.teleport(lobbySpawn);
        return true;
    }

    @Override
    public void removePlayer(final Player player, final boolean toLobby) {
        final UUID uuid = player.getUniqueId();
        players.remove(uuid);
        spectators.remove(uuid);
        spawnProtectionUntil.remove(uuid);
        playerArmorTier.remove(uuid);
        playerAxeTier.remove(uuid);
        playerPickaxeTier.remove(uuid);
        playerHasShears.remove(uuid);

        getTeam(player).ifPresent(team -> team.removePlayer(player));
    }

    @Override
    public boolean contains(final Player player) {
        return players.contains(player.getUniqueId()) || spectators.contains(player.getUniqueId());
    }

    @Override
    public void tryStartCountdown() {
        if (gameState != EGameState.LOBBY_WAITING) {
            return;
        }
        final int playerCount = players.size();
        if (playerCount < minPlayers) {
            return;
        }

        final int threshold = (int) Math.ceil(maxPlayers * 0.75D);
        if (playerCount < threshold) {
            return;
        }

        gameState = EGameState.STARTING;

        if (startingTask != null) {
            startingTask.cancel();
        }
        // Base countdown is 30 seconds once 75% of slots are filled.
        startingTask = new GameStartingTask(this, 30);
        startingTask.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void forceStart() {
        if (gameState == EGameState.IN_GAME) {
            return;
        }
        gameState = EGameState.IN_GAME;
        gameStartMillis = System.currentTimeMillis();
        diamondTier = 1;
        emeraldTier = 1;
        generatorPhase = GeneratorPhase.NORMAL;

        // Assign all players to teams randomly, then teleport (addPlayer teleports to team spawn).
        assignAllPlayersToTeamsRandomly();

        // Empty teams have no bed from the start: red X on scoreboard/list; their generators still run.
        for (ITeam team : teams) {
            if (team.getMemberIds().isEmpty()) {
                team.setBedDestroyed(true);
            }
        }

        for (Player p : getPlayers()) {
            giveStarterKit(p);
            sendGameStartIntro(p);
        }

        clearLobbyRegion();

        if (playingTask != null) {
            playingTask.cancel();
        }
        playingTask = new GamePlayingTask(this);
        playingTask.runTaskTimer(plugin, 20L, 20L);
    }

    private static void sendGameStartIntro(final Player player) {
        final String separator = ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        player.sendMessage(separator);
        player.sendMessage(centerChat(ChatColor.WHITE + "" + ChatColor.BOLD + "Bed Wars"));
        player.sendMessage("");
        player.sendMessage(centerChat(ChatColor.YELLOW + "" + ChatColor.BOLD + "Protect your bed and destroy the enemy beds."));
        player.sendMessage(centerChat(ChatColor.YELLOW + "" + ChatColor.BOLD + "Upgrade yourself and your team by collecting"));
        player.sendMessage(centerChat(ChatColor.YELLOW + "" + ChatColor.BOLD + "Iron, Gold, Emerald and Diamond from generators"));
        player.sendMessage(centerChat(ChatColor.YELLOW + "" + ChatColor.BOLD + "to access powerful upgrades."));
        player.sendMessage("");
        player.sendMessage(separator);
    }

    /**
     * Recolors the bed at the given location so that both halves use the team's bed color
     * while preserving head/foot parts and facing, so it looks like a normal bed.
     */
    private static void applyTeamBedColor(final World world, final Location bedLocation, final ETeamColor color) {
        if (world == null || bedLocation == null || bedLocation.getWorld() == null) {
            return;
        }
        final Block block = world.getBlockAt(bedLocation);
        final BlockData data = block.getBlockData();
        if (!(data instanceof Bed bedData)) {
            return;
        }
        final Block otherHalf = getOtherBedHalf(block, bedData);
        final org.bukkit.Material bedMaterial = color.getBedMaterial();
        final org.bukkit.block.data.type.Bed.Part part1 = bedData.getPart();
        final org.bukkit.block.BlockFace facing = bedData.getFacing();

        // Determine part of the other half from its current data (if it's a bed), otherwise infer.
        Bed otherData = null;
        if (otherHalf.getBlockData() instanceof Bed od) {
            otherData = od;
        }
        final org.bukkit.block.data.type.Bed.Part part2 = otherData != null
                ? otherData.getPart()
                : (part1 == Bed.Part.HEAD ? Bed.Part.FOOT : Bed.Part.HEAD);

        final Bed newFirst = (Bed) Bukkit.createBlockData(bedMaterial);
        newFirst.setFacing(facing);
        newFirst.setPart(part1);

        final Bed newSecond = (Bed) Bukkit.createBlockData(bedMaterial);
        newSecond.setFacing(facing);
        newSecond.setPart(part2);

        block.setBlockData(newFirst, false);
        otherHalf.setBlockData(newSecond, false);
    }

    private static Block getOtherBedHalf(final Block block, final Bed bedData) {
        final org.bukkit.block.BlockFace facing = bedData.getFacing();
        final org.bukkit.block.BlockFace offset;
        if (bedData.getPart() == Bed.Part.HEAD) {
            offset = facing.getOppositeFace();
        } else {
            offset = facing;
        }
        return block.getRelative(offset);
    }

    /**
     * Called when the global Bed Break event triggers. Destroys all remaining beds
     * (both halves, marking them for map restore) and routes through the normal
     * bed-destroyed flow so titles and chat are sent.
     */
    private void handleGlobalBedBreak() {
        final Bedwars bw = plugin instanceof Bedwars ? (Bedwars) plugin : Bedwars.getInstance();
        if (bw == null) {
            return;
        }
        final com.kartersanamo.bedwars.maprestore.InternalAdapter adapter = bw.getInternalAdapter();

        for (ITeam team : teams) {
            if (team.isBedDestroyed()) {
                continue;
            }
            final Location bedLoc = team.getBedLocation();
            if (bedLoc == null || bedLoc.getWorld() == null || !bedLoc.getWorld().equals(world)) {
                continue;
            }
            final Block bedBlock = world.getBlockAt(bedLoc);
            final BlockData data = bedBlock.getBlockData();
            if (data instanceof Bed bedData) {
                final Block otherHalf = getOtherBedHalf(bedBlock, bedData);
                adapter.markModified(this, bedBlock);
                adapter.markModified(this, otherHalf);
                bedBlock.setType(org.bukkit.Material.AIR, false);
                otherHalf.setType(org.bukkit.Material.AIR, false);
            } else {
                adapter.markModified(this, bedBlock);
                bedBlock.setType(org.bukkit.Material.AIR, false);
            }
            // breaker = null so no player stats, but titles/chat still fire.
            handleBedDestroyed(team, null);
        }
    }

    private void startSuddenDeath() {
        if (suddenDeathStarted) {
            return;
        }
        suddenDeathStarted = true;

        // Cinematic announcement.
        for (Player p : getPlayers()) {
            p.sendTitle(ChatColor.DARK_RED + "☠ SUDDEN DEATH ☠",
                    ChatColor.YELLOW + "Dragons have been unleashed!", 10, 80, 10);
            p.sendMessage(ChatColor.GRAY + " ");
            p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "SUDDEN DEATH > "
                    + ChatColor.YELLOW + "Ender Dragons have spawned!");
            p.sendMessage(ChatColor.GRAY + " ");
            // Feather Falling effect for the whole phase.
        }
        for (Player p : getSpectators()) {
            p.sendMessage(ChatColor.GRAY + " ");
            p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "SUDDEN DEATH > "
                    + ChatColor.YELLOW + "Ender Dragons have spawned!");
            p.sendMessage(ChatColor.GRAY + " ");
        }

        // One dragon per non-eliminated team at start.
        for (ITeam team : teams) {
            if (team.isEliminated()) {
                continue;
            }
            spawnDragonForTeam(team);
        }
    }

    private void updateSuddenDeathDragons(final long elapsedSeconds, final GeneratorsConfig config) {
        final int suddenDeathAt = config.getTierUpgradeSuddenDeathSeconds();
        final long sinceSuddenDeath = Math.max(0, elapsedSeconds - suddenDeathAt);

        // 0–5 min of Sudden Death: 1 dragon per team; after 5 min: 2 dragons per team.
        final int targetPerTeam = sinceSuddenDeath >= 300 ? 2 : 1;

        for (ITeam team : teams) {
            if (team.isEliminated()) {
                continue;
            }
            ensureDragonCountForTeam(team, targetPerTeam);
        }
    }

    private void ensureDragonCountForTeam(final ITeam team, final int target) {
        final List<UUID> list = teamDragonIds.computeIfAbsent(team.getId(), k -> new ArrayList<>());
        final Iterator<UUID> it = list.iterator();
        while (it.hasNext()) {
            final UUID id = it.next();
            final org.bukkit.entity.Entity e = Bukkit.getEntity(id);
            if (!(e instanceof org.bukkit.entity.EnderDragon dragon) || dragon.isDead()) {
                it.remove();
            }
        }
        int count = list.size();
        while (count < target) {
            final UUID id = spawnDragonForTeam(team);
            if (id == null) {
                break;
            }
            list.add(id);
            count++;
        }
    }

    private UUID spawnDragonForTeam(final ITeam team) {
        if (world == null) {
            return null;
        }
        final Location base = team.getSpawnLocation();
        if (base == null || base.getWorld() == null || !base.getWorld().equals(world)) {
            return null;
        }
        final Location spawn = base.clone().add(0.0, 25.0, 0.0);
        final org.bukkit.entity.EnderDragon dragon = world.spawn(spawn, org.bukkit.entity.EnderDragon.class, d -> {
            d.setCustomNameVisible(true);
            d.setCustomName(team.getColor().getChatColor() + team.getId() + " Dragon");
        });
        return dragon.getUniqueId();
    }

    private void despawnAllTeamDragons() {
        if (world == null) {
            return;
        }
        for (List<UUID> list : teamDragonIds.values()) {
            for (UUID id : list) {
                final org.bukkit.entity.Entity e = Bukkit.getEntity(id);
                if (e != null && !e.isDead()) {
                    e.remove();
                }
            }
        }
    }

    /**
     * Ends the game due to the absolute time cap being reached. All surviving teams/players
     * are treated as winners for stats; broadcast summary with no single winning team.
     */
    private void endGameAsDraw() {
        if (gameState == EGameState.ENDING || gameState == EGameState.RESETTING) {
            return;
        }
        gameState = EGameState.ENDING;

        // Treat all non-eliminated teams as "winners" for stats purposes.
        broadcastGameOverSummary(null);
        if (playingTask != null) {
            playingTask.cancel();
            playingTask = null;
        }
        final Bedwars bedwars = plugin instanceof Bedwars ? (Bedwars) plugin : Bedwars.getInstance();
        if (bedwars != null) {
            new GameRestartingTask(bedwars, this, 10).runTaskTimer(plugin, 20L, 20L);
        }
    }

    /**
     * Roughly center a chat line by prefixing spaces based on its visible length.
     * This mimics Hypixel-style centered intro text.
     */
    private static String centerChat(final String message) {
        final String stripped = ChatColor.stripColor(message);
        // Typical Minecraft chat fits ~55 characters comfortably; use that as a baseline.
        final int chatWidth = 55;
        final int msgLength = stripped != null ? stripped.length() : 0;
        final int padding = Math.max(0, (chatWidth - msgLength) / 2);
        if (padding == 0) {
            return message;
        }
        return " ".repeat(padding) + message;
    }

    private static final int RESPAWN_SPECTATOR_SECONDS = 3;
    private static final int SPAWN_PROTECTION_SECONDS = 2;

    @Override
    public void handlePlayerDeath(final Player player, final Player killer) {
        player.teleport(spectatorSpawn);
        player.setGameMode(GameMode.SPECTATOR);

        final Optional<ITeam> teamOpt = getTeam(player);
        final boolean bedDestroyed = teamOpt.map(ITeam::isBedDestroyed).orElse(true);

        if (bedDestroyed) {
            player.sendMessage(ChatColor.RED + "You have been eliminated!");
            players.remove(player.getUniqueId());
            spectators.add(player.getUniqueId());
            if (teamOpt.isPresent()) {
                final ITeam eliminatedTeam = teamOpt.get();
                eliminatedTeam.removePlayer(player);
                boolean anyLeft = false;
                for (UUID memberId : eliminatedTeam.getMemberIds()) {
                    if (players.contains(memberId)) {
                        anyLeft = true;
                        break;
                    }
                }
                if (!anyLeft) {
                    final String colorName = eliminatedTeam.getColor().name().charAt(0) + eliminatedTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);
                    final String msg = ChatColor.WHITE.toString() + ChatColor.BOLD + "TEAM ELIMINATED > "
                            + eliminatedTeam.getColor().getChatColor() + colorName + " Team"
                            + ChatColor.GRAY + " has been eliminated!";
                    for (Player p : getPlayers()) {
                        p.sendMessage("");
                        p.sendMessage(msg);
                        p.sendMessage("");
                    }
                    for (Player p : getSpectators()) {
                        p.sendMessage("");
                        p.sendMessage(msg);
                        p.sendMessage("");
                    }
                }
            }
            checkGameOver();
            return;
        }

        // Respawn flow: title/subtitle + chat countdown, then respawn at base after RESPAWN_SPECTATOR_SECONDS.
        player.sendTitle(
                ChatColor.RED + "YOU DIED!",
                ChatColor.YELLOW + "You will respawn in " + ChatColor.RED +  RESPAWN_SPECTATOR_SECONDS + ChatColor.YELLOW + " seconds!",
                10, 70, 20
        );
        player.sendMessage(ChatColor.YELLOW + "You will respawn in " + RESPAWN_SPECTATOR_SECONDS + " seconds!");
        for (int s = RESPAWN_SPECTATOR_SECONDS - 1; s >= 1; s--) {
            final int secondsLeft = s;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!players.contains(player.getUniqueId())) {
                    return;
                }
                final String msg = secondsLeft == 1
                        ? ChatColor.YELLOW + "You will respawn in " + ChatColor.RED + "1" + ChatColor.YELLOW + " second!"
                        : ChatColor.YELLOW + "You will respawn in " + ChatColor.RED + secondsLeft + ChatColor.YELLOW + " seconds!";
                player.sendMessage(msg);
                player.sendTitle(
                        ChatColor.RED + "YOU DIED!",
                        ChatColor.YELLOW + "You will respawn in " + ChatColor.RED + secondsLeft
                                + ChatColor.YELLOW + (secondsLeft == 1 ? " second!" : " seconds!"),
                        0, 40, 10
                );
            }, (RESPAWN_SPECTATOR_SECONDS - secondsLeft) * 20L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnAtBase(player);
            if (players.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "You have respawned!");
                player.sendTitle(ChatColor.GREEN + "RESPAWNED", "", 0, 20, 10);
            }
        }, RESPAWN_SPECTATOR_SECONDS * 20L);
    }

    /**
     * Teleports the player to their team spawn, restores health/food, and grants spawn protection.
     * Call only when the player is in this arena and their team's bed is still up.
     */
    private void respawnAtBase(final Player player) {
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) {
            return;
        }
        if (!players.contains(player.getUniqueId())) {
            return;
        }
        final Optional<ITeam> teamOpt = getTeam(player);
        if (teamOpt.isEmpty() || teamOpt.get().isBedDestroyed()) {
            // Bed was destroyed during the 3-second wait; eliminate now.
            players.remove(player.getUniqueId());
            spectators.add(player.getUniqueId());
            player.teleport(spectatorSpawn);
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }
        final ITeam team = teamOpt.get();
        player.teleport(team.getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        player.setFoodLevel(20);
        player.getInventory().clear();
        giveRespawnKit(player);
        spawnProtectionUntil.put(player.getUniqueId(), System.currentTimeMillis() + SPAWN_PROTECTION_SECONDS * 1000L);
    }

    @Override
    public void handleBedDestroyed(final ITeam victimTeam, final Player breaker) {
        victimTeam.setBedDestroyed(true);
        if (breaker != null) {
            recordBedBroken(breaker.getUniqueId());
            if (plugin instanceof Bedwars bw) {
                try {
                    bw.getDatabase().getCachedStats(breaker.getUniqueId(), breaker.getName()).incrementBedsBroken();
                } catch (Exception ignored) {
                }
            }
        }
        final String colorName = victimTeam.getColor().name().charAt(0) + victimTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);

        // Notify players on the destroyed team with a title.
        for (Player p : victimTeam.getOnlineMembers()) {
            p.sendTitle(ChatColor.RED + "BED DESTROYED", ChatColor.WHITE + "You will no longer respawn", 10, 60, 10);
        }

        final String message;
        if (breaker != null) {
            message = ChatColor.WHITE.toString() + ChatColor.BOLD + "BED DESTRUCTION > "
                    + victimTeam.getColor().getChatColor() + colorName + " Bed"
                    + ChatColor.GRAY + " was destroyed by "
                    + breaker.getName() + "!";
        } else {
            message = ChatColor.WHITE.toString() + ChatColor.BOLD + "BED DESTRUCTION > "
                    + victimTeam.getColor().getChatColor() + colorName + " Bed"
                    + ChatColor.GRAY + " was destroyed!";
        }
        for (Player p : getPlayers()) {
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
        }
        for (Player p : getSpectators()) {
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
        }
    }

    @Override
    public void resetAfterGame() {
        for (Player p : getPlayers()) {
            p.getEnderChest().clear();
        }
        for (Player p : getSpectators()) {
            p.getEnderChest().clear();
        }
        players.clear();
        spectators.clear();
        spawnProtectionUntil.clear();
        playerArmorTier.clear();
        playerAxeTier.clear();
        playerPickaxeTier.clear();
        playerHasShears.clear();
        for (ITeam team : teams) {
            team.resetTeam();
            final TeamUpgradeState state = teamUpgradeStates.get(team.getId());
            if (state != null) {
                state.reset();
            }
        }
        playersInsideEnemyBase.clear();
        killCountThisGame.clear();
        finalKillsThisGame.clear();
        bedsBrokenThisGame.clear();
        diamondTier = 1;
        emeraldTier = 1;
        generatorPhase = GeneratorPhase.NORMAL;
        suddenDeathStarted = false;
        gameOverWarningSent = false;
        despawnAllTeamDragons();
        teamDragonIds.values().forEach(List::clear);
        gameState = EGameState.LOBBY_WAITING;
    }

    /**
     * Checks win condition: if at most one team is left (not eliminated), ends the game.
     * Call after a player is eliminated (final kill) and from GamePlayingTask.
     */
    public void checkGameOver() {
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) {
            return;
        }
        final List<ITeam> aliveTeams = teams.stream()
                .filter(team -> !team.isEliminated())
                .toList();
        if (aliveTeams.size() > 1) {
            return;
        }
        gameState = EGameState.ENDING;
        final ITeam winningTeam = aliveTeams.isEmpty() ? null : aliveTeams.getFirst();
        broadcastGameOverSummary(winningTeam);
        if (playingTask != null) {
            playingTask.cancel();
            playingTask = null;
        }
        final Bedwars bedwars = plugin instanceof Bedwars ? (Bedwars) plugin : Bedwars.getInstance();
        if (bedwars != null) {
            new GameRestartingTask(bedwars, this, 10).runTaskTimer(plugin, 20L, 20L);
        }
    }

    /** Called every second (or tick) to apply tier upgrades when time is reached. */
    public void checkTierUpgrades() {
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) {
            return;
        }
        final GeneratorsConfig config = plugin instanceof Bedwars ? ((Bedwars) plugin).getGeneratorsConfig() : null;
        if (config == null) {
            return;
        }
        final long elapsedSeconds = (System.currentTimeMillis() - gameStartMillis) / 1000L;

        if (diamondTier < 2 && elapsedSeconds >= config.getTierUpgradeDiamond2Seconds()) {
            diamondTier = 2;
        }
        if (emeraldTier < 2 && elapsedSeconds >= config.getTierUpgradeEmerald2Seconds()) {
            emeraldTier = 2;
        }
        if (diamondTier < 3 && elapsedSeconds >= config.getTierUpgradeDiamond3Seconds()) {
            diamondTier = 3;
        }
        if (emeraldTier < 3 && elapsedSeconds >= config.getTierUpgradeEmerald3Seconds()) {
            emeraldTier = 3;
        }
        if (generatorPhase == GeneratorPhase.NORMAL && elapsedSeconds >= config.getTierUpgradeBedBreakSeconds()) {
            generatorPhase = GeneratorPhase.BED_BREAK;
            handleGlobalBedBreak();
        }
        if (generatorPhase != GeneratorPhase.SUDDEN_DEATH && elapsedSeconds >= config.getTierUpgradeSuddenDeathSeconds()) {
            generatorPhase = GeneratorPhase.SUDDEN_DEATH;
            startSuddenDeath();
        }

        if (generatorPhase == GeneratorPhase.SUDDEN_DEATH) {
            updateSuddenDeathDragons(elapsedSeconds, config);
        }

        // Absolute game-time cap: after Sudden Death has been running for 10 minutes (total 50 minutes from start),
        // end the game as a draw. All remaining players are treated as winners for stats.
        final int gameOverAt = config.getTierUpgradeGameOverSeconds();
        if (!gameOverWarningSent && elapsedSeconds >= gameOverAt - 60) {
            gameOverWarningSent = true;
            for (Player p : getPlayers()) {
                p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game ends in 1 minute!");
            }
        }
        if (elapsedSeconds >= gameOverAt) {
            endGameAsDraw();
        }
    }

    @Override
    public int getDiamondTier() {
        return diamondTier;
    }

    @Override
    public int getEmeraldTier() {
        return emeraldTier;
    }

    @Override
    public int getEffectiveDiamondIntervalTicks() {
        final GeneratorsConfig config = plugin instanceof Bedwars ? ((Bedwars) plugin).getGeneratorsConfig() : null;
        if (config == null) return 600;
        return switch (diamondTier) {
            case 2 -> config.getDiamondTier2IntervalTicks();
            case 3 -> config.getDiamondTier3IntervalTicks();
            default -> config.getDiamondIntervalTicks();
        };
    }

    @Override
    public int getEffectiveEmeraldIntervalTicks() {
        final GeneratorsConfig config = plugin instanceof Bedwars ? ((Bedwars) plugin).getGeneratorsConfig() : null;
        if (config == null) return 1200;
        return switch (emeraldTier) {
            case 2 -> config.getEmeraldTier2IntervalTicks();
            case 3 -> config.getEmeraldTier3IntervalTicks();
            default -> config.getEmeraldIntervalTicks();
        };
    }

    @Override
    public String getNextTierUpgradeMessage() {
        final GeneratorsConfig config = plugin instanceof Bedwars ? ((Bedwars) plugin).getGeneratorsConfig() : null;
        if (config == null) return ChatColor.GRAY + "—";
        final long elapsedSeconds = (System.currentTimeMillis() - gameStartMillis) / 1000L;

        if (diamondTier < 2) {
            final int at = config.getTierUpgradeDiamond2Seconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Diamond II in " + ChatColor.GREEN + formatTime(rem);
        }
        if (emeraldTier < 2) {
            final int at = config.getTierUpgradeEmerald2Seconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Emerald II in " + ChatColor.GREEN + formatTime(rem);
        }
        if (diamondTier < 3) {
            final int at = config.getTierUpgradeDiamond3Seconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Diamond III in " + ChatColor.GREEN + formatTime(rem);
        }
        if (emeraldTier < 3) {
            final int at = config.getTierUpgradeEmerald3Seconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Emerald III in " + ChatColor.GREEN + formatTime(rem);
        }
        if (generatorPhase == GeneratorPhase.NORMAL) {
            final int at = config.getTierUpgradeBedBreakSeconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Bed Break in " + ChatColor.GREEN + formatTime(rem);
        }
        if (generatorPhase == GeneratorPhase.BED_BREAK) {
            final int at = config.getTierUpgradeSuddenDeathSeconds();
            final long rem = Math.max(0, at - elapsedSeconds);
            return ChatColor.WHITE + "Sudden Death in " + ChatColor.GREEN + formatTime(rem);
        }
        final int gameOverAt = config.getTierUpgradeGameOverSeconds();
        final long rem = Math.max(0, gameOverAt - elapsedSeconds);
        return ChatColor.WHITE + "Game Over in " + ChatColor.GREEN + formatTime(rem);
    }

    private static String formatTime(final long totalSeconds) {
        final long m = totalSeconds / 60;
        final long s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    @Override
    public void recordKill(final UUID killerUniqueId) {
        if (killerUniqueId == null) return;
        killCountThisGame.merge(killerUniqueId, 1, Integer::sum);
    }

    @Override
    public void recordFinalKill(final UUID killerUniqueId) {
        if (killerUniqueId == null) return;
        finalKillsThisGame.merge(killerUniqueId, 1, Integer::sum);
    }

    /** Records a bed broken by the given player (this game only). */
    public void recordBedBroken(final UUID playerUniqueId) {
        if (playerUniqueId == null) return;
        bedsBrokenThisGame.merge(playerUniqueId, 1, Integer::sum);
    }

    @Override
    public int getKillsThisGame(final UUID playerUniqueId) {
        return killCountThisGame.getOrDefault(playerUniqueId, 0);
    }

    @Override
    public int getFinalKillsThisGame(final UUID playerUniqueId) {
        return finalKillsThisGame.getOrDefault(playerUniqueId, 0);
    }

    @Override
    public int getBedsBrokenThisGame(final UUID playerUniqueId) {
        return bedsBrokenThisGame.getOrDefault(playerUniqueId, 0);
    }

    @Override
    public void broadcastGameOverSummary(final ITeam winningTeam) {
        final String separator = ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        for (Player p : getPlayers()) {
            if (winningTeam != null && winningTeam.getOnlineMembers().contains(p)) {
                p.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "VICTORY!", "", 10, 70, 20);
            }
            p.sendMessage(separator);
            p.sendMessage(centerChat(ChatColor.WHITE + "" + ChatColor.BOLD + "Bed Wars"));
            p.sendMessage("");
            if (winningTeam != null) {
                final String names = String.join(", ", winningTeam.getOnlineMembers().stream().map(Player::getName).toList());
                p.sendMessage(centerChat(winningTeam.getColor().getChatColor() + "Winners" + ChatColor.GRAY + " - " + names));
            }
            p.sendMessage("");
            sendKillerStats(p);
            p.sendMessage(separator);
        }
        for (Player p : getSpectators()) {
            p.sendMessage(separator);
            p.sendMessage(ChatColor.WHITE + "Bed Wars");
            p.sendMessage(separator);
            if (winningTeam != null) {
                final String colorName = winningTeam.getColor().name().charAt(0) + winningTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);
                final String names = String.join(", ", winningTeam.getOnlineMembers().stream().map(Player::getName).toList());
                p.sendMessage(winningTeam.getColor().getChatColor() + colorName + ChatColor.WHITE + " - " + names);
            }
            sendKillerStats(p);
            p.sendMessage("");
            p.sendMessage(separator);
        }
    }

    private void sendKillerStats(final Player recipient) {
        final List<Player> all = new ArrayList<>();
        all.addAll(getPlayers());
        all.addAll(getSpectators());
        all.removeIf(p -> killCountThisGame.getOrDefault(p.getUniqueId(), 0) <= 0);
        all.sort(Comparator.comparingInt(p -> -killCountThisGame.getOrDefault(p.getUniqueId(), 0)));
        final ChatColor[] rankColors = { ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED };
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            final Player p = all.get(i);
            final int count = killCountThisGame.getOrDefault(p.getUniqueId(), 0);
            final String rank = (i + 1) + (i == 0 ? "st" : i == 1 ? "nd" : "rd");
            recipient.sendMessage(centerChat(rankColors[i] + "" + ChatColor.BOLD + rank + " Killer" + ChatColor.GRAY + " - " + p.getName() + " - " + count));
        }
    }

    // --- Kit state for shop upgrades (armor/axe/pickaxe persist; sword resets on death) ---

    public ArmorTier getPlayerArmorTier(final Player player) {
        return playerArmorTier.getOrDefault(player.getUniqueId(), ArmorTier.LEATHER);
    }

    /** Upgrades armor tier only if the new tier is higher. Returns true if upgraded. */
    public boolean setPlayerArmorTier(final Player player, final ArmorTier tier) {
        final ArmorTier current = getPlayerArmorTier(player);
        if (tier.ordinal() <= current.ordinal()) {
            return false;
        }
        playerArmorTier.put(player.getUniqueId(), tier);
        return true;
    }

    public ToolTier getPlayerAxeTier(final Player player) {
        return playerAxeTier.getOrDefault(player.getUniqueId(), ToolTier.NONE);
    }

    public void setPlayerAxeTier(final Player player, final ToolTier tier) {
        playerAxeTier.put(player.getUniqueId(), tier);
    }

    public ToolTier getPlayerPickaxeTier(final Player player) {
        return playerPickaxeTier.getOrDefault(player.getUniqueId(), ToolTier.NONE);
    }

    public void setPlayerPickaxeTier(final Player player, final ToolTier tier) {
        playerPickaxeTier.put(player.getUniqueId(), tier);
    }

    public boolean hasShears(final Player player) {
        return playerHasShears.contains(player.getUniqueId());
    }

    public void setPlayerHasShears(final Player player, final boolean has) {
        if (has) {
            playerHasShears.add(player.getUniqueId());
        } else {
            playerHasShears.remove(player.getUniqueId());
        }
    }

    @Override
    public boolean hasSpawnProtection(final Player player) {
        final Long until = spawnProtectionUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            spawnProtectionUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * Cancels the lobby countdown if one is running. Call when cancelling a
     * game that has not started yet.
     */
    public void cancelCountdown() {
        if (startingTask != null) {
            startingTask.cancel();
            startingTask = null;
        }
        gameState = EGameState.LOBBY_WAITING;
    }

    /**
     * Shuffles all current players and assigns them to teams in round-robin order.
     * Each player is then added to their team (which teleports them to the team spawn).
     * Call only when transitioning from lobby/starting to in-game.
     */
    private void assignAllPlayersToTeamsRandomly() {
        final List<Player> playerList = new ArrayList<>(getPlayers());
        Collections.shuffle(playerList, new Random());
        final List<ITeam> teamList = new ArrayList<>(teams);
        if (teamList.isEmpty()) {
            return;
        }
        for (int i = 0; i < playerList.size(); i++) {
            final ITeam team = teamList.get(i % teamList.size());
            team.addPlayer(playerList.get(i));
        }
    }

    private static final Material[] SWORD_MATERIALS = {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD
    };

    private static boolean isSword(final Material type) {
        for (Material m : SWORD_MATERIALS) {
            if (m == type) return true;
        }
        return false;
    }

    /** Gives a wooden sword and full leather armor (team-colored chest/helmet). Axe/pickaxe are shop-only. */
    private void giveStarterKit(final Player player) {
        final UUID uuid = player.getUniqueId();
        playerArmorTier.put(uuid, ArmorTier.LEATHER);
        playerAxeTier.put(uuid, ToolTier.NONE);
        playerPickaxeTier.put(uuid, ToolTier.NONE);
        playerHasShears.remove(uuid);

        player.getInventory().clear();
        player.getInventory().setItem(0, unbreakable(new ItemStack(Material.WOODEN_SWORD)));
        applyArmor(player);
    }

    /** After death: wooden sword, armor (from tier), axe/pickaxe only if purchased (degraded), shears if owned. */
    private void giveRespawnKit(final Player player) {
        final UUID uuid = player.getUniqueId();
        ToolTier axeTier = getPlayerAxeTier(player).degradeAxe();
        ToolTier pickTier = getPlayerPickaxeTier(player).degradePickaxe();
        playerAxeTier.put(uuid, axeTier);
        playerPickaxeTier.put(uuid, pickTier);

        player.getInventory().setItem(0, unbreakable(new ItemStack(Material.WOODEN_SWORD)));
        applyArmor(player);
        if (axeTier.hasTool()) {
            player.getInventory().addItem(unbreakable(new ItemStack(axeTier.getAxeMaterial())));
        }
        if (pickTier.hasTool()) {
            player.getInventory().addItem(unbreakable(new ItemStack(pickTier.getPickaxeMaterial())));
        }
        if (playerHasShears.contains(uuid)) {
            player.getInventory().addItem(unbreakable(new ItemStack(Material.SHEARS)));
        }
    }

    private void applyArmor(final Player player) {
        removeKitArmorFromInventory(player);
        final Optional<ITeam> teamOpt = getTeam(player);
        final ETeamColor color = teamOpt.map(ITeam::getColor).orElse(ETeamColor.WHITE);
        final ArmorTier tier = getPlayerArmorTier(player);
        final TeamUpgradeState upgradeState = teamOpt.map(this::getTeamUpgradeState).orElse(null);
        final int protLevel = upgradeState != null ? upgradeState.getProtection() : 0;

        final ItemStack chest = unbreakable(new ItemStack(Material.LEATHER_CHESTPLATE));
        final ItemStack helmet = unbreakable(new ItemStack(Material.LEATHER_HELMET));
        setLeatherColor(chest, color);
        setLeatherColor(helmet, color);
        if (protLevel > 0) {
            chest.addUnsafeEnchantment(Enchantment.PROTECTION, protLevel);
            helmet.addUnsafeEnchantment(Enchantment.PROTECTION, protLevel);
        }

        final ItemStack legs = unbreakable(new ItemStack(tier.getLeggings()));
        final ItemStack boots = unbreakable(new ItemStack(tier.getBoots()));
        setLeatherColor(legs, color);
        setLeatherColor(boots, color);
        if (protLevel > 0) {
            legs.addUnsafeEnchantment(Enchantment.PROTECTION, protLevel);
            boots.addUnsafeEnchantment(Enchantment.PROTECTION, protLevel);
        }

        player.getInventory().setChestplate(chest);
        player.getInventory().setHelmet(helmet);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);
    }

    private static void setLeatherColor(final ItemStack stack, final ETeamColor color) {
        final ItemMeta meta = stack.getItemMeta();
        if (meta instanceof LeatherArmorMeta lam) {
            lam.setColor(color.getDyeColor().getColor());
            stack.setItemMeta(lam);
        }
    }

    private static ItemStack unbreakable(final ItemStack stack) {
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Removes any unbreakable armor from the player's storage/hotbar to prevent duplication when reapplying. */
    private static void removeKitArmorFromInventory(final Player player) {
        final org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            final ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getItemMeta() != null && stack.getItemMeta().isUnbreakable() && isArmorType(stack.getType())) {
                inv.setItem(i, null);
            }
        }
    }

    private static boolean isArmorType(final Material type) {
        if (type == null) return false;
        final String n = type.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    /** Ensures player has at least one sword; if none, adds wooden. Call from enforcement task/listener. */
    public void ensureSword(final Player player) {
        int count = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && isSword(s.getType())) count += s.getAmount();
        }
        if (count < 1) {
            final ItemStack sword = unbreakable(new ItemStack(Material.WOODEN_SWORD));
            applySharpnessIfTeamHas(player, sword);
            player.getInventory().addItem(sword);
        } else {
            applySharpnessToAllSwords(player);
        }
    }

    /** Applies Protection to current armor and Sharpness to all swords based on team upgrades. Call after shop buy or when reapplying. */
    public void applyTeamUpgradesToInventory(final Player player) {
        final Optional<ITeam> teamOpt = getTeam(player);
        if (teamOpt.isEmpty()) return;
        final TeamUpgradeState state = getTeamUpgradeState(teamOpt.get());
        if (state == null) return;
        final int prot = state.getProtection();
        if (prot > 0) {
            for (ItemStack piece : new ItemStack[]{
                    player.getInventory().getHelmet(),
                    player.getInventory().getChestplate(),
                    player.getInventory().getLeggings(),
                    player.getInventory().getBoots()
            }) {
                if (piece != null && !piece.getType().isAir()) {
                    piece.addUnsafeEnchantment(Enchantment.PROTECTION, prot);
                }
            }
        }
        applySharpnessToAllSwords(player);
    }

    private void applySharpnessToAllSwords(final Player player) {
        final Optional<ITeam> teamOpt = getTeam(player);
        if (teamOpt.isEmpty()) return;
        if (!getTeamUpgradeState(teamOpt.get()).hasSharpness()) return;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && isSword(s.getType()) && !s.getEnchantments().containsKey(Enchantment.SHARPNESS)) {
                s.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
            }
        }
    }

    private void applySharpnessIfTeamHas(final Player player, final ItemStack sword) {
        final Optional<ITeam> teamOpt = getTeam(player);
        if (teamOpt.isEmpty()) return;
        if (getTeamUpgradeState(teamOpt.get()).hasSharpness()) {
            sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
        }
    }

    /**
     * Call when a player moves. If they entered an enemy base and that team has a trap, trigger it.
     */
    public void checkTrapTrigger(final Player player) {
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) return;
        final Optional<ITeam> playerTeamOpt = getTeam(player);
        if (playerTeamOpt.isEmpty()) return;
        final ITeam playerTeam = playerTeamOpt.get();
        final Location loc = player.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().equals(world)) return;

        for (ITeam team : teams) {
            if (team.getId().equals(playerTeam.getId()) || team.isBedDestroyed()) continue;
            final Location bed = team.getBedLocation();
            if (bed == null || bed.getWorld() == null || !bed.getWorld().equals(world)) continue;
            final boolean inBase = loc.distanceSquared(bed) <= (BASE_TRAP_RADIUS * BASE_TRAP_RADIUS);
            final Set<UUID> inside = playersInsideEnemyBase.computeIfAbsent(team.getId(), k -> new HashSet<>());
            if (inBase) {
                if (!inside.contains(player.getUniqueId())) {
                    inside.add(player.getUniqueId());
                    triggerTrap(team, player);
                }
            } else {
                inside.remove(player.getUniqueId());
            }
        }
    }

    private void triggerTrap(final ITeam defendingTeam, final Player enemy) {
        final TeamUpgradeState state = getTeamUpgradeState(defendingTeam);
        if (state == null) return;
        final TrapType trap = state.consumeNextTrap();
        if (trap == null) return;
        switch (trap) {
            case ITS_A_TRAP -> {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8 * 20, 0));
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 8 * 20, 0));
            }
            case COUNTER_OFFENSIVE -> {
                for (Player p : defendingTeam.getOnlineMembers()) {
                    if (p.getWorld().equals(world) && p.getLocation().distanceSquared(defendingTeam.getBedLocation()) <= (BASE_TRAP_RADIUS * BASE_TRAP_RADIUS)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10 * 20, 1));
                    }
                }
            }
            case ALARM -> {
                enemy.removePotionEffect(PotionEffectType.INVISIBILITY);
                for (Player p : defendingTeam.getOnlineMembers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                }
            }
            case MINER_FATIGUE -> enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10 * 20, 0));
            default -> { }
        }
    }

    /** Applies Haste (if team has it) and Regeneration at base (if team has Heal Pool). Call periodically. */
    public void applyHasteAndHealPool(final Player player) {
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) return;
        final Optional<ITeam> teamOpt = getTeam(player);
        if (teamOpt.isEmpty()) return;
        final TeamUpgradeState state = getTeamUpgradeState(teamOpt.get());
        if (state == null) return;
        final int haste = state.getHaste();
        if (haste > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, haste - 1, true, false));
        }
        if (state.hasHealPool()) {
            final Location spawn = teamOpt.get().getSpawnLocation();
            if (spawn != null && spawn.getWorld() != null && player.getWorld().equals(spawn.getWorld())
                    && player.getLocation().distanceSquared(spawn) <= (HEAL_POOL_RADIUS * HEAL_POOL_RADIUS)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
            }
        }
    }

    /** Re-applies permanent armor (chest/helmet team color, legs/boots from tier). Call when armor was removed. */
    public void reapplyArmorIfNeeded(final Player player) {
        if (!players.contains(player.getUniqueId()) && !spectators.contains(player.getUniqueId())) {
            return;
        }
        if (gameState != EGameState.IN_GAME && gameState != EGameState.ENDING) {
            return;
        }
        applyArmor(player);
    }

    private void clearLobbyRegion() {
        if (lobbyRegion == null || world == null) {
            return;
        }

        final Location pos1 = lobbyRegion.getPos1();
        final Location pos2 = lobbyRegion.getPos2();

        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR, false);
                }
            }
        }
    }
}
