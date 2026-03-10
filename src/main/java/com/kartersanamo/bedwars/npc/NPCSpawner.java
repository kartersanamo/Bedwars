package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.api.arena.EGameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single NPC that players can click to open the game GUI.
 */
public final class NPCSpawner {

    private final UUID uuid = UUID.randomUUID();
    private final Location location;
    private final EGameMode gameMode;
    private final List<ArmorStand> hologramStands = new ArrayList<>();
    private ArmorStand npcArmorStand;
    private Entity visualNpc;

    public NPCSpawner(final Location location, final EGameMode gameMode) {
        this.location = location.clone();
        this.gameMode = gameMode;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location.clone();
    }

    public EGameMode getGameMode() {
        return gameMode;
    }

    public ArmorStand getNpcArmorStand() {
        return npcArmorStand;
    }

    public void setNpcArmorStand(final ArmorStand npcArmorStand) {
        this.npcArmorStand = npcArmorStand;
    }

    public Entity getVisualNpc() {
        return visualNpc;
    }

    public void setVisualNpc(final Entity visualNpc) {
        this.visualNpc = visualNpc;
    }

    public void addHologramStand(final ArmorStand stand) {
        hologramStands.add(stand);
    }

    public List<ArmorStand> getHologramStands() {
        return new ArrayList<>(hologramStands);
    }

    public boolean isValid() {
        if (npcArmorStand == null || npcArmorStand.isDead()) {
            return false;
        }
        if (visualNpc == null || visualNpc.isDead()) {
            return false;
        }
        for (ArmorStand stand : hologramStands) {
            if (stand == null || stand.isDead()) {
                return false;
            }
        }
        return true;
    }

    public void remove() {
        if (npcArmorStand != null && !npcArmorStand.isDead()) {
            npcArmorStand.remove();
        }
        if (visualNpc != null && !visualNpc.isDead()) {
            visualNpc.remove();
        }
        for (ArmorStand stand : hologramStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        hologramStands.clear();
        npcArmorStand = null;
        visualNpc = null;
    }
}
