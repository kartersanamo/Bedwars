package com.kartersanamo.bedwars.arena.team;

import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Default team implementation for the MVP.
 */
public final class BedwarsTeam implements ITeam {

    private final String id;
    private final ETeamColor color;
    private final Location spawnLocation;
    private final Location bedLocation;
    private final JavaPlugin plugin;

    private final Set<UUID> memberIds = new HashSet<>();
    private boolean bedDestroyed;

    public BedwarsTeam(final String id,
                       final ETeamColor color,
                       final Location spawnLocation,
                       final Location bedLocation,
                       final JavaPlugin plugin) {
        this.id = Objects.requireNonNull(id, "id");
        this.color = Objects.requireNonNull(color, "color");
        this.spawnLocation = Objects.requireNonNull(spawnLocation, "spawnLocation");
        this.bedLocation = Objects.requireNonNull(bedLocation, "bedLocation");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ETeamColor getColor() {
        return color;
    }

    @Override
    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    @Override
    public Location getBedLocation() {
        return bedLocation.clone();
    }

    @Override
    public boolean isBedDestroyed() {
        return bedDestroyed;
    }

    @Override
    public void setBedDestroyed(final boolean destroyed) {
        this.bedDestroyed = destroyed;
    }

    @Override
    public boolean isEliminated() {
        if (!bedDestroyed) {
            return false;
        }
        // Eliminated when there are no living members left.
        for (UUID uuid : memberIds) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Collection<UUID> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    @Override
    public Collection<Player> getOnlineMembers() {
        final List<Player> result = new ArrayList<>();
        for (UUID uuid : memberIds) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    @Override
    public void addPlayer(final Player player) {
        memberIds.add(player.getUniqueId());
        player.teleport(spawnLocation);
    }

    @Override
    public void removePlayer(final Player player) {
        memberIds.remove(player.getUniqueId());
    }

    @Override
    public boolean contains(final Player player) {
        return memberIds.contains(player.getUniqueId());
    }

    @Override
    public void resetTeam() {
        memberIds.clear();
        bedDestroyed = false;
    }
}
