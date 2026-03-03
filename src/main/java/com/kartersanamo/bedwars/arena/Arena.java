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
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        }

        clearLobbyRegion();

        if (playingTask != null) {
            playingTask.cancel();
        }
        playingTask = new GamePlayingTask(this);
        playingTask.runTaskTimer(plugin, 20L, 20L);
    }

    private static final int RESPAWN_SPECTATOR_SECONDS = 3;
    private static final int SPAWN_PROTECTION_SECONDS = 2;

    @Override
    public void handlePlayerDeath(final Player player, final Player killer) {
        final Optional<ITeam> teamOpt = getTeam(player);
        final boolean bedDestroyed = teamOpt.map(ITeam::isBedDestroyed).orElse(true);

        if (bedDestroyed) {
            // Eliminated: move to spectators permanently.
            players.remove(player.getUniqueId());
            spectators.add(player.getUniqueId());
            player.teleport(spectatorSpawn);
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }

        // Respawn flow: spectator for 3 seconds, then respawn at base with 2 seconds spawn protection.
        player.setGameMode(GameMode.SPECTATOR);
        // Keep in players; do not add to spectators.
        Bukkit.getScheduler().runTaskLater(plugin, () -> respawnAtBase(player), RESPAWN_SPECTATOR_SECONDS * 20L);
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
        }
        gameState = EGameState.LOBBY_WAITING;
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

        final ItemStack chest = unbreakable(new ItemStack(Material.LEATHER_CHESTPLATE));
        final ItemStack helmet = unbreakable(new ItemStack(Material.LEATHER_HELMET));
        setLeatherColor(chest, color);
        setLeatherColor(helmet, color);

        final ItemStack legs = unbreakable(new ItemStack(tier.getLeggings()));
        final ItemStack boots = unbreakable(new ItemStack(tier.getBoots()));

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
            player.getInventory().addItem(unbreakable(new ItemStack(Material.WOODEN_SWORD)));
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
