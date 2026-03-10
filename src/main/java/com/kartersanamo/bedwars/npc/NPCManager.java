package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.*;

/**
 * Manages all NPC spawners in the plugin.
 */
public final class NPCManager {

    private final Bedwars plugin;
    private final Map<UUID, NPCSpawner> spawners = new HashMap<>();
    private final Map<UUID, NPCSpawner> spawnersByArmorStandId = new HashMap<>();
    private final Map<UUID, NPCSpawner> spawnersByEntityId = new HashMap<>();
    private final Set<UUID> playersInRemoveMode = new HashSet<>();

    public NPCManager(final Bedwars plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new NPC at the given location for the given game mode.
     */
    public NPCSpawner spawnNPC(final Location location, final EGameMode gameMode) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a valid world");
        }

        final NPCSpawner spawner = new NPCSpawner(location, gameMode);

        final Villager visualNpc = location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setSilent(true);
            villager.setProfession(Villager.Profession.NONE);
            villager.setCustomNameVisible(false);
        });
        spawner.setVisualNpc(visualNpc);
        spawnersByEntityId.put(visualNpc.getUniqueId(), spawner);

        final ArmorStand npc = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.setCustomNameVisible(false);
        });
        spawner.setNpcArmorStand(npc);
        spawnersByArmorStandId.put(npc.getUniqueId(), spawner);
        spawnersByEntityId.put(npc.getUniqueId(), spawner);

        createHologram(location, gameMode, spawner);

        spawners.put(spawner.getUuid(), spawner);
        return spawner;
    }

    /**
     * Creates a hologram above the NPC showing game mode and player count.
     */
    private void createHologram(final Location baseLocation, final EGameMode gameMode, final NPCSpawner spawner) {
        final World world = baseLocation.getWorld();
        if (world == null) return;

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
        });
        spawner.addHologramStand(line2);

        final ArmorStand line3 = world.spawn(new Location(world, x, y - 0.50, z), ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setSmall(true);
            updateHologramPlayerCount(armorStand, gameMode);
        });
        spawner.addHologramStand(line3);
    }

    /**
     * Updates the player count on the hologram for a given game mode.
     */
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

    /**
     * Updates all holograms to reflect current player counts.
     */
    public void updateHolograms() {
        for (NPCSpawner spawner : spawners.values()) {
            if (!spawner.isValid()) {
                continue;
            }
            final List<ArmorStand> stands = spawner.getHologramStands();
            if (stands.size() >= 3) {
                final ArmorStand playerCountLine = stands.get(2);
                updateHologramPlayerCount(playerCountLine, spawner.getGameMode());
            }
        }
    }

    /**
     * Removes an NPC by its UUID.
     */
    public boolean removeNPC(final UUID uuid) {
        final NPCSpawner spawner = spawners.remove(uuid);
        if (spawner != null) {
            final ArmorStand npc = spawner.getNpcArmorStand();
            if (npc != null) {
                spawnersByArmorStandId.remove(npc.getUniqueId());
                spawnersByEntityId.remove(npc.getUniqueId());
            }
            final Entity visualNpc = spawner.getVisualNpc();
            if (visualNpc != null) {
                spawnersByEntityId.remove(visualNpc.getUniqueId());
            }
            spawner.remove();
            return true;
        }
        return false;
    }

    /**
     * Removes an NPC by its entity (armor stand or visual NPC).
     */
    public boolean removeNPCByEntity(final Entity entity) {
        final NPCSpawner spawner = spawnersByEntityId.get(entity.getUniqueId());
        if (spawner == null) {
            return false;
        }
        return removeNPC(spawner.getUuid());
    }

    /**
     * Gets an NPC spawner by its entity (armor stand or visual NPC).
     */
    public NPCSpawner getNPCByEntity(final Entity entity) {
        return spawnersByEntityId.get(entity.getUniqueId());
    }

    /**
     * Gets all spawned NPCs for a specific game mode.
     */
    public List<NPCSpawner> getNPCsForMode(final EGameMode gameMode) {
        final List<NPCSpawner> result = new ArrayList<>();
        for (NPCSpawner spawner : spawners.values()) {
            if (spawner.getGameMode() == gameMode && spawner.isValid()) {
                result.add(spawner);
            }
        }
        return result;
    }

    /**
     * Gets all spawned NPCs.
     */
    public Collection<NPCSpawner> getAllNPCs() {
        return new ArrayList<>(spawners.values());
    }

    /**
     * Clears all NPCs.
     */
    public void clearAll() {
        for (NPCSpawner spawner : spawners.values()) {
            spawner.remove();
        }
        spawners.clear();
        spawnersByArmorStandId.clear();
        spawnersByEntityId.clear();
    }

    /**
     * Sets whether a player is in remove mode.
     */
    public void setRemoveMode(final Player player, final boolean enabled) {
        if (enabled) {
            playersInRemoveMode.add(player.getUniqueId());
        } else {
            playersInRemoveMode.remove(player.getUniqueId());
        }
    }

    /**
     * Checks if a player is in remove mode.
     */
    public boolean isInRemoveMode(final Player player) {
        return playersInRemoveMode.contains(player.getUniqueId());
    }

}

