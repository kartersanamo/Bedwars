package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import com.kartersanamo.bedwars.arena.team.BedwarsTeam;
import com.kartersanamo.bedwars.arena.tasks.GamePlayingTask;
import com.kartersanamo.bedwars.arena.tasks.GameRestartingTask;
import com.kartersanamo.bedwars.arena.tasks.GameStartingTask;
import com.kartersanamo.bedwars.sidebar.SidebarService;
import com.kartersanamo.bedwars.upgrades.TeamUpgradeState;
import com.kartersanamo.bedwars.upgrades.TrapType;
import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.GeneratorsConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Concrete arena implementation.
 *
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

    /** Per-team diamond upgrade and trap state. */
    private final Map<String, TeamUpgradeState> teamUpgradeStates = new HashMap<>();

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

    public Optional<ArenaConfig.Region> getRegion() {
        return Optional.ofNullable(region);
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
        if (region == null || location == null || !location.getWorld().equals(world)) {
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

        if (toLobby) {
            // The global lobby spawn is outside the responsibility of the arena;
            // the caller should handle teleportation appropriately.
        }
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
        final String separator = ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        player.sendMessage(separator);
        player.sendTitle(
                ChatColor.WHITE + "Bed Wars",
                ChatColor.YELLOW + "Protect your bed and destroy the enemy beds.",
                10, 70, 20
        );
        player.sendMessage(ChatColor.YELLOW + "Protect your bed and destroy the enemy beds.");
        player.sendMessage(ChatColor.YELLOW + "Upgrade yourself and your team by collecting");
        player.sendMessage(ChatColor.YELLOW + "Iron, Gold, Emerald and Diamond from generators");
        player.sendMessage(ChatColor.YELLOW + "to access powerful upgrades.");
        player.sendMessage(separator);
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
            final Optional<ITeam> eliminatedTeamOpt = teamOpt;
            players.remove(player.getUniqueId());
            spectators.add(player.getUniqueId());
            if (eliminatedTeamOpt.isPresent()) {
                final ITeam eliminatedTeam = eliminatedTeamOpt.get();
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
                            + ChatColor.WHITE + " has been eliminated!";
                    for (Player p : getPlayers()) {
                        p.sendMessage(msg);
                    }
                    for (Player p : getSpectators()) {
                        p.sendMessage(msg);
                    }
                }
            }
            return;
        }

        // Respawn flow: title/subtitle + chat countdown, then respawn at base after RESPAWN_SPECTATOR_SECONDS.
        player.sendTitle(
                ChatColor.RED + "YOU DIED!",
                ChatColor.YELLOW + "You will respawn in " + RESPAWN_SPECTATOR_SECONDS + " seconds!",
                10, 70, 20
        );
        player.sendMessage(ChatColor.YELLOW + "You will respawn in " + RESPAWN_SPECTATOR_SECONDS + " seconds!");
        for (int s = RESPAWN_SPECTATOR_SECONDS - 1; s >= 1; s--) {
            final int secondsLeft = s;
            final String msg = s == 1
                    ? ChatColor.YELLOW + "You will respawn in 1 second!"
                    : ChatColor.YELLOW + "You will respawn in " + secondsLeft + " seconds!";
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendMessage(msg), (RESPAWN_SPECTATOR_SECONDS - secondsLeft) * 20L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnAtBase(player);
            if (players.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "You have respawned!");
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
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.getInventory().clear();
        giveRespawnKit(player);
        spawnProtectionUntil.put(player.getUniqueId(), System.currentTimeMillis() + SPAWN_PROTECTION_SECONDS * 1000L);
    }

    @Override
    public void handleBedDestroyed(final ITeam victimTeam, final Player breaker) {
        victimTeam.setBedDestroyed(true);
        if (plugin instanceof Bedwars bw) {
            try {
                bw.getDatabase().getCachedStats(breaker.getUniqueId(), breaker.getName()).incrementBedsBroken();
            } catch (Exception ignored) {
            }
        }
        final String colorName = victimTeam.getColor().name().charAt(0) + victimTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);
        final String message = ChatColor.WHITE.toString() + ChatColor.BOLD + "BED DESTRUCTION > "
                + victimTeam.getColor().getChatColor() + colorName + " Bed"
                + ChatColor.WHITE + " was destroyed by " + breaker.getName() + "!";
        for (Player p : getPlayers()) {
            p.sendMessage(message);
        }
        for (Player p : getSpectators()) {
            p.sendMessage(message);
        }
    }

    @Override
    public void resetAfterGame() {
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
        diamondTier = 1;
        emeraldTier = 1;
        generatorPhase = GeneratorPhase.NORMAL;
        gameState = EGameState.LOBBY_WAITING;
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
        }
        if (generatorPhase != GeneratorPhase.SUDDEN_DEATH && elapsedSeconds >= config.getTierUpgradeSuddenDeathSeconds()) {
            generatorPhase = GeneratorPhase.SUDDEN_DEATH;
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
        return ChatColor.GRAY + "Sudden Death";
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
    public void broadcastGameOverSummary(final ITeam winningTeam) {
        final String separator = ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        for (Player p : getPlayers()) {
            p.sendMessage(separator);
            p.sendMessage(ChatColor.WHITE + "Bed Wars");
            if (winningTeam != null) {
                final String colorName = winningTeam.getColor().name().charAt(0) + winningTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);
                final String names = String.join(", ", winningTeam.getOnlineMembers().stream().map(Player::getName).toList());
                p.sendMessage(ChatColor.WHITE + colorName + " - " + names);
            }
            sendKillerStats(p);
            p.sendMessage(separator);
        }
        for (Player p : getSpectators()) {
            p.sendMessage(separator);
            p.sendMessage(ChatColor.WHITE + "Bed Wars");
            if (winningTeam != null) {
                final String colorName = winningTeam.getColor().name().charAt(0) + winningTeam.getColor().name().substring(1).toLowerCase(Locale.ROOT);
                final String names = String.join(", ", winningTeam.getOnlineMembers().stream().map(Player::getName).toList());
                p.sendMessage(ChatColor.WHITE + colorName + " - " + names);
            }
            sendKillerStats(p);
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
            recipient.sendMessage(rankColors[i] + rank + " Killer - " + ChatColor.WHITE + p.getName() + " - " + count);
        }
    }

    // --- Kit state for shop upgrades (armor/axe/pickaxe persist; sword resets on death) ---

    public ArmorTier getPlayerArmorTier(final Player player) {
        return playerArmorTier.getOrDefault(player.getUniqueId(), ArmorTier.LEATHER);
    }

    /** Upgrades armor tier only if new tier is higher. Returns true if upgraded. */
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

    private static boolean isPermanentArmor(final ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        return stack.getType().name().endsWith("_HELMET") || stack.getType().name().endsWith("_CHESTPLATE")
                || stack.getType().name().endsWith("_LEGGINGS") || stack.getType().name().endsWith("_BOOTS");
    }

    /** Gives wooden sword and full leather armor (team-colored chest/helmet). Axe/pickaxe are shop-only. */
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
                if (piece != null && !piece.getType().isAir() && piece.getType().getEquipmentSlot() != null) {
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
            case MINER_FATIGUE -> {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 10 * 20, 0));
            }
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
