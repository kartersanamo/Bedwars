package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Manages all NPC spawners in the plugin.
 */
public final class NPCManager {

    private static final String ROLE_VISUAL = "visual";
    private static final String ROLE_CLICK = "click";
    private static final String ROLE_LINE_1 = "line1";
    private static final String ROLE_LINE_2 = "line2";
    private static final String ROLE_LINE_3 = "line3";

    private final Bedwars plugin;
    private final Map<UUID, NPCSpawner> spawners = new HashMap<>();
    private final Map<UUID, NPCSpawner> spawnersByEntityId = new HashMap<>();
    private final Set<UUID> playersInRemoveMode = new HashSet<>();

    private final NamespacedKey npcIdKey;
    private final NamespacedKey npcModeKey;
    private final NamespacedKey npcRoleKey;

    public NPCManager(final Bedwars plugin) {
        this.plugin = plugin;
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
        this.npcModeKey = new NamespacedKey(plugin, "npc_mode");
        this.npcRoleKey = new NamespacedKey(plugin, "npc_role");
    }

    public NPCSpawner spawnNPC(final Location location, final EGameMode gameMode) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a valid world");
        }

        final NPCSpawner spawner = new NPCSpawner(location, gameMode);
        final String npcId = spawner.getUuid().toString();

        final Villager visualNpc = location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setSilent(true);
            villager.setProfession(Villager.Profession.NONE);
            villager.setCustomNameVisible(false);
            tagEntity(villager, npcId, gameMode, ROLE_VISUAL);
        });
        spawner.setVisualNpc(visualNpc);

        final ArmorStand clickStand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.setCustomNameVisible(false);
            tagEntity(armorStand, npcId, gameMode, ROLE_CLICK);
        });
        spawner.setNpcArmorStand(clickStand);

        createHologram(location, gameMode, spawner, npcId);
        registerSpawner(spawner);
        return spawner;
    }

    private void createHologram(final Location baseLocation,
                                final EGameMode gameMode,
                                final NPCSpawner spawner,
                                final String npcId) {
        final World world = baseLocation.getWorld();
        if (world == null) {
            return;
        }

        final double x = baseLocation.getX();
        final double y = baseLocation.getY() + 2.5;
        final double z = baseLocation.getZ();

        final ArmorStand line1 = world.spawn(new Location(world, x, y, z), ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setSmall(true);
            armorStand.setCustomName("" + ChatColor.YELLOW + ChatColor.BOLD + "CLICK TO PLAY");
            tagEntity(armorStand, npcId, gameMode, ROLE_LINE_1);
        });
        spawner.addHologramStand(line1);

        final ArmorStand line2 = world.spawn(new Location(world, x, y - 0.25, z), ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setSmall(true);
            armorStand.setCustomName(ChatColor.AQUA + gameMode.getDisplayName()
                    + ChatColor.GRAY + " [v" + plugin.getDescription().getVersion() + "]");
            tagEntity(armorStand, npcId, gameMode, ROLE_LINE_2);
        });
        spawner.addHologramStand(line2);

        final ArmorStand line3 = world.spawn(new Location(world, x, y - 0.50, z), ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setSmall(true);
            updateHologramPlayerCount(armorStand, gameMode);
            tagEntity(armorStand, npcId, gameMode, ROLE_LINE_3);
        });
        spawner.addHologramStand(line3);
    }

    /**
     * Rebuild in-memory NPC mappings from tagged entities in loaded worlds.
     * Also performs a legacy recovery pass for old NPCs without tags.
     */
    public void loadExistingNPCs() {
        clearRuntimeCache();

        final Map<String, LoadedNpcParts> grouped = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                final PersistentDataContainer pdc = entity.getPersistentDataContainer();
                final String npcId = pdc.get(npcIdKey, PersistentDataType.STRING);
                final String modeName = pdc.get(npcModeKey, PersistentDataType.STRING);
                final String role = pdc.get(npcRoleKey, PersistentDataType.STRING);

                if (npcId == null || modeName == null || role == null) {
                    continue;
                }

                final EGameMode gameMode = parseGameMode(modeName);
                if (gameMode == null) {
                    continue;
                }

                final LoadedNpcParts parts = grouped.computeIfAbsent(npcId, k -> new LoadedNpcParts(gameMode));
                parts.gameMode = gameMode;
                parts.attach(role, entity);
            }
        }

        int loaded = 0;
        for (Map.Entry<String, LoadedNpcParts> entry : grouped.entrySet()) {
            final LoadedNpcParts parts = entry.getValue();
            if (!parts.isComplete()) {
                continue;
            }
            final Location base = parts.clickStand.getLocation();
            final NPCSpawner spawner = new NPCSpawner(base, parts.gameMode);
            spawner.setNpcArmorStand(parts.clickStand);
            spawner.setVisualNpc(parts.visualNpc);
            spawner.addHologramStand(parts.line1);
            spawner.addHologramStand(parts.line2);
            spawner.addHologramStand(parts.line3);
            registerSpawner(spawner);
            loaded++;
        }

        final int recoveredLegacy = recoverLegacyUntaggedNPCs();
        final int total = loaded + recoveredLegacy;
        if (total > 0) {
            plugin.getLogger().info("Loaded " + total + " persisted NPC(s) on startup (tagged=" + loaded
                    + ", legacyRecovered=" + recoveredLegacy + ").");
        }
    }

    /**
     * Best-effort recovery for NPCs created before entity tags existed.
     */
    private int recoverLegacyUntaggedNPCs() {
        int recovered = 0;

        for (World world : Bukkit.getWorlds()) {
            final java.util.Collection<ArmorStand> stands = world.getEntitiesByClass(ArmorStand.class);
            for (ArmorStand line1 : stands) {
                if (line1.getPersistentDataContainer().has(npcIdKey, PersistentDataType.STRING)) {
                    continue;
                }
                final String line1Name = ChatColor.stripColor(line1.getCustomName());
                if (line1Name == null || !line1Name.equalsIgnoreCase("CLICK TO PLAY")) {
                    continue;
                }

                final ArmorStand line2 = findNearbyHologramLine(line1, 0.35, 0.20, 0.40, s -> {
                    final String n = ChatColor.stripColor(s.getCustomName());
                    return n != null && n.contains("[v");
                });
                final ArmorStand line3 = findNearbyHologramLine(line1, 0.65, 0.40, 0.50, s -> {
                    final String n = ChatColor.stripColor(s.getCustomName());
                    return n != null && n.toLowerCase(Locale.ROOT).contains("players");
                });
                if (line2 == null || line3 == null) {
                    continue;
                }

                final EGameMode mode = parseModeFromVersionLine(line2.getCustomName());
                if (mode == null) {
                    continue;
                }

                final Location baseEstimate = line1.getLocation().clone().subtract(0.0, 2.5, 0.0);
                final Villager villager = findNearbyVillager(baseEstimate);
                final ArmorStand click = findNearbyClickStand(baseEstimate);
                if (villager == null || click == null) {
                    continue;
                }

                // Skip if this entity is already tracked from tagged load.
                if (spawnersByEntityId.containsKey(villager.getUniqueId()) || spawnersByEntityId.containsKey(click.getUniqueId())) {
                    continue;
                }

                final NPCSpawner spawner = new NPCSpawner(click.getLocation(), mode);
                spawner.setVisualNpc(villager);
                spawner.setNpcArmorStand(click);
                spawner.addHologramStand(line1);
                spawner.addHologramStand(line2);
                spawner.addHologramStand(line3);

                // Tag recovered entities so future restarts are deterministic.
                final String npcId = spawner.getUuid().toString();
                tagEntity(villager, npcId, mode, ROLE_VISUAL);
                tagEntity(click, npcId, mode, ROLE_CLICK);
                tagEntity(line1, npcId, mode, ROLE_LINE_1);
                tagEntity(line2, npcId, mode, ROLE_LINE_2);
                tagEntity(line3, npcId, mode, ROLE_LINE_3);

                registerSpawner(spawner);
                recovered++;
            }
        }

        return recovered;
    }

    private ArmorStand findNearbyHologramLine(final ArmorStand anchor,
                                              final double yDown,
                                              final double yTolerance,
                                              final double radius,
                                              final java.util.function.Predicate<ArmorStand> predicate) {
        for (ArmorStand stand : anchor.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (stand == anchor) {
                continue;
            }
            if (Math.abs(stand.getLocation().getX() - anchor.getLocation().getX()) > radius
                    || Math.abs(stand.getLocation().getZ() - anchor.getLocation().getZ()) > radius) {
                continue;
            }
            final double expectedY = anchor.getLocation().getY() - yDown;
            if (Math.abs(stand.getLocation().getY() - expectedY) > yTolerance) {
                continue;
            }
            if (!predicate.test(stand)) {
                continue;
            }
            return stand;
        }
        return null;
    }

    private Villager findNearbyVillager(final Location center) {
        for (Villager villager : Objects.requireNonNull(center.getWorld()).getEntitiesByClass(Villager.class)) {
            if (villager.getLocation().distanceSquared(center) <= 1.2 * 1.2) {
                return villager;
            }
        }
        return null;
    }

    private ArmorStand findNearbyClickStand(final Location center) {
        for (ArmorStand stand : Objects.requireNonNull(center.getWorld()).getEntitiesByClass(ArmorStand.class)) {
            if (!stand.isMarker() || !stand.isInvisible()) {
                continue;
            }
            if (stand.getCustomName() != null) {
                continue;
            }
            if (stand.getLocation().distanceSquared(center) <= 1.0) {
                return stand;
            }
        }
        return null;
    }

    private EGameMode parseModeFromVersionLine(final String customName) {
        if (customName == null) {
            return null;
        }
        final String stripped = ChatColor.stripColor(customName);
        for (EGameMode mode : EGameMode.values()) {
            if (stripped.toLowerCase(Locale.ROOT).startsWith(mode.getDisplayName().toLowerCase(Locale.ROOT))) {
                return mode;
            }
        }
        return null;
    }

    private EGameMode parseGameMode(final String raw) {
        for (EGameMode mode : EGameMode.values()) {
            if (mode.name().equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return null;
    }

    private void registerSpawner(final NPCSpawner spawner) {
        spawners.put(spawner.getUuid(), spawner);
        final Entity visual = spawner.getVisualNpc();
        if (visual != null) {
            spawnersByEntityId.put(visual.getUniqueId(), spawner);
        }
        final ArmorStand click = spawner.getNpcArmorStand();
        if (click != null) {
            spawnersByEntityId.put(click.getUniqueId(), spawner);
        }
        for (ArmorStand hologram : spawner.getHologramStands()) {
            spawnersByEntityId.put(hologram.getUniqueId(), spawner);
        }
    }

    private void tagEntity(final Entity entity, final String npcId, final EGameMode gameMode, final String role) {
        final PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(npcIdKey, PersistentDataType.STRING, npcId);
        pdc.set(npcModeKey, PersistentDataType.STRING, gameMode.name());
        pdc.set(npcRoleKey, PersistentDataType.STRING, role);
    }

    private void updateHologramPlayerCount(final ArmorStand hologram, final EGameMode gameMode) {
        int playerCount = 0;
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getTeamSize() != gameMode.getTeamSize()) {
                continue;
            }
            if (arena.getGameState() == EGameState.IN_GAME || arena.getGameState() == EGameState.STARTING
                    || arena.getGameState() == EGameState.LOBBY_WAITING) {
                playerCount += arena.getPlayers().size();
            }
        }
        final String displayName = "" + ChatColor.YELLOW + ChatColor.BOLD + playerCount + ChatColor.YELLOW + " Players";
        hologram.setCustomName(displayName);
    }

    public void updateHolograms() {
        for (NPCSpawner spawner : spawners.values()) {
            if (!spawner.isValid()) {
                continue;
            }
            final List<ArmorStand> stands = spawner.getHologramStands();
            if (stands.size() >= 3) {
                updateHologramPlayerCount(stands.get(2), spawner.getGameMode());
            }
        }
    }

    public boolean removeNPC(final UUID uuid) {
        final NPCSpawner spawner = spawners.remove(uuid);
        if (spawner == null) {
            return false;
        }
        unregisterSpawner(spawner);
        spawner.remove();
        return true;
    }

    public void removeNPCByEntity(final Entity entity) {
        final NPCSpawner spawner = spawnersByEntityId.get(entity.getUniqueId());
        if (spawner == null) {
            return;
        }
        removeNPC(spawner.getUuid());
    }

    public NPCSpawner getNPCByEntity(final Entity entity) {
        return spawnersByEntityId.get(entity.getUniqueId());
    }

    public List<NPCSpawner> getNPCsForMode(final EGameMode gameMode) {
        final List<NPCSpawner> result = new ArrayList<>();
        for (NPCSpawner spawner : spawners.values()) {
            if (spawner.getGameMode() == gameMode && spawner.isValid()) {
                result.add(spawner);
            }
        }
        return result;
    }

    public java.util.Collection<NPCSpawner> getAllNPCs() {
        return new ArrayList<>(spawners.values());
    }

    /**
     * Non-destructive shutdown clear: only removes in-memory mappings.
     */
    public void clearRuntimeCache() {
        spawners.clear();
        spawnersByEntityId.clear();
        playersInRemoveMode.clear();
    }

    private void unregisterSpawner(final NPCSpawner spawner) {
        final Entity visual = spawner.getVisualNpc();
        if (visual != null) {
            spawnersByEntityId.remove(visual.getUniqueId());
        }
        final ArmorStand click = spawner.getNpcArmorStand();
        if (click != null) {
            spawnersByEntityId.remove(click.getUniqueId());
        }
        for (ArmorStand hologram : spawner.getHologramStands()) {
            spawnersByEntityId.remove(hologram.getUniqueId());
        }
    }

    public void setRemoveMode(final Player player, final boolean enabled) {
        if (enabled) {
            playersInRemoveMode.add(player.getUniqueId());
        } else {
            playersInRemoveMode.remove(player.getUniqueId());
        }
    }

    public boolean isInRemoveMode(final Player player) {
        return playersInRemoveMode.contains(player.getUniqueId());
    }

    /**
     * Repairs runtime NPC mappings by rescanning persisted entities in loaded worlds.
     *
     * @return number of NPCs currently mapped after repair
     */
    public int repairRuntimeMappings() {
        loadExistingNPCs();
        return spawners.size();
    }

    private static final class LoadedNpcParts {
        private EGameMode gameMode;
        private Villager visualNpc;
        private ArmorStand clickStand;
        private ArmorStand line1;
        private ArmorStand line2;
        private ArmorStand line3;

        private LoadedNpcParts(final EGameMode gameMode) {
            this.gameMode = gameMode;
        }

        private void attach(final String role, final Entity entity) {
            if (ROLE_VISUAL.equals(role) && entity instanceof Villager villager) {
                this.visualNpc = villager;
                return;
            }
            if (!(entity instanceof ArmorStand stand)) {
                return;
            }
            switch (role) {
                case ROLE_CLICK -> this.clickStand = stand;
                case ROLE_LINE_1 -> this.line1 = stand;
                case ROLE_LINE_2 -> this.line2 = stand;
                case ROLE_LINE_3 -> this.line3 = stand;
                default -> {
                }
            }
        }

        private boolean isComplete() {
            return gameMode != null && visualNpc != null && clickStand != null
                    && line1 != null && line2 != null && line3 != null;
        }
    }
}
