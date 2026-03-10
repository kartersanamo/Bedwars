package com.kartersanamo.bedwars.arena.team;

import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Default team implementation for the MVP.
 */
public final class BedwarsTeam implements ITeam {

    private final String id;
    private final ETeamColor color;
    private final Location spawnLocation;
    private final Location bedLocation;

    private final Set<UUID> memberIds = new HashSet<>();
    private boolean bedDestroyed;

    public BedwarsTeam(final String id,
                       final ETeamColor color,
                       final Location spawnLocation,
                       final Location bedLocation) {
        this.id = Objects.requireNonNull(id, "id");
        this.color = Objects.requireNonNull(color, "color");
        this.spawnLocation = Objects.requireNonNull(spawnLocation, "spawnLocation");
        this.bedLocation = Objects.requireNonNull(bedLocation, "bedLocation");
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
        // A team is eliminated when their bed is gone AND they have no members remaining.
        // Member removal happens when a player is fully eliminated from the arena
        // (moved to spectators set), so an empty memberIds means nobody can respawn.
        return bedDestroyed && memberIds.isEmpty();
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
