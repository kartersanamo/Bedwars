package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.team.BedwarsTeam;
import com.kartersanamo.bedwars.arena.tasks.GamePlayingTask;
import com.kartersanamo.bedwars.arena.tasks.GameRestartingTask;
import com.kartersanamo.bedwars.arena.tasks.GameStartingTask;
import com.kartersanamo.bedwars.sidebar.SidebarService;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
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

    private final List<ITeam> teams;
    private final List<IGenerator> generators = new ArrayList<>();

    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();

    private final JavaPlugin plugin;

    private boolean enabled = true;
    private EGameState gameState = EGameState.LOBBY_WAITING;

    private GameStartingTask startingTask;
    private GamePlayingTask playingTask;

    private Arena(final String id,
                  final String displayName,
                  final World world,
                  final Location lobbySpawn,
                  final Location spectatorSpawn,
                  final int minPlayers,
                  final int maxPlayers,
                  final int teamSize,
                  final ArenaConfig.Region region,
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
        this.teams = Collections.unmodifiableList(new ArrayList<>(teams));
        this.plugin = plugin;
    }

    public static Arena fromConfig(final ArenaConfig config, final MainConfig mainConfig, final JavaPlugin plugin) {
        final World world = config.getWorld();
        final String id = config.getId();

        final Location lobbySpawn = Optional.ofNullable(config.getLobbySpawn())
                .orElseGet(() -> world != null ? world.getSpawnLocation() : null);
        final Location spectatorSpawn = Optional.ofNullable(config.getSpectatorSpawn())
                .orElse(lobbySpawn);

        final int minPlayers = config.getMinPlayers(mainConfig.getDefaultMinPlayers());
        final int maxPlayers = config.getMaxPlayers(mainConfig.getDefaultMaxPlayers());
        final int teamSize = config.getTeamSize(mainConfig.getDefaultTeamSize());
        final ArenaConfig.Region region = config.getArenaRegion().orElse(null);

        final List<ITeam> teams = new ArrayList<>();
        for (ArenaConfig.TeamDefinition def : config.getTeamDefinitions()) {
            teams.add(new BedwarsTeam(def.getId(), def.getColor(), def.getSpawn(), def.getBed(), plugin));
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

    @Override
    public List<ITeam> getTeams() {
        return teams;
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
        players.remove(player.getUniqueId());
        spectators.remove(player.getUniqueId());

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
        if (players.size() < minPlayers) {
            return;
        }
        gameState = EGameState.STARTING;

        if (startingTask != null) {
            startingTask.cancel();
        }
        startingTask = new GameStartingTask(this, 20);
        startingTask.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void forceStart() {
        if (gameState == EGameState.IN_GAME) {
            return;
        }
        gameState = EGameState.IN_GAME;

        // Teleport all players to their team spawn.
        for (Player player : getPlayers()) {
            getTeam(player).ifPresent(team -> player.teleport(team.getSpawnLocation()));
        }

        if (playingTask != null) {
            playingTask.cancel();
        }
        playingTask = new GamePlayingTask(this);
        playingTask.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void handlePlayerDeath(final Player player, final Player killer) {
        // Detailed respawn and elimination logic will be implemented in the
        // dedicated task/listener layer. For now we simply mark the player
        // as spectator; this will be expanded as the MVP is fleshed out.
        players.remove(player.getUniqueId());
        spectators.add(player.getUniqueId());
        player.teleport(spectatorSpawn);
    }

    @Override
    public void handleBedDestroyed(final ITeam victimTeam, final Player breaker) {
        victimTeam.setBedDestroyed(true);
    }

    @Override
    public void resetAfterGame() {
        players.clear();
        spectators.clear();
        gameState = EGameState.LOBBY_WAITING;
    }
}
